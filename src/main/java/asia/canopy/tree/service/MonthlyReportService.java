package asia.canopy.tree.service;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.MonthlyReport;
import asia.canopy.tree.domain.QType;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.MonthlyReportDto;
import asia.canopy.tree.exception.BadRequestException;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.DiaryRepository;
import asia.canopy.tree.repository.MonthlyReportRepository;
import asia.canopy.tree.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${google.cse.api-key}")
    private String googleCseApiKey;

    @Value("${google.cse.engine-id}")
    private String googleCseEngineId;

    /**
     * 특정 월의 리포트를 조회하고, 없으면 생성합니다. (기존 리포트 우선 활용)
     *
     * @param userId 사용자 ID
     * @param year   연도
     * @param month  월
     * @return 월간 리포트 DTO
     */
    @Transactional(readOnly = true)
    public MonthlyReportDto getOrCreateMonthlyReport(Long userId, int year, int month) {
        // 기존 리포트 조회
        Optional<MonthlyReport> existingReport = monthlyReportRepository.findByUserUserIdAndYearAndMonth(userId, year, month);

        if (existingReport.isPresent()) {
            log.info("기존 리포트를 반환합니다: 사용자 ID={}, 연도={}, 월={}", userId, year, month);
            return convertToDto(existingReport.get());
        } else {
            // 리포트가 없으면 새로 생성
            return generateMonthlyReport(userId, year, month);
        }
    }

    /**
     * 특정 월의 일기를 분석하여 월간 리포트를 생성합니다. 이미 생성된 리포트가 있더라도 새로 생성합니다.
     *
     * @param userId 사용자 ID
     * @param year   연도
     * @param month  월
     * @return 월간 리포트 DTO
     */
    @Transactional
    public MonthlyReportDto generateMonthlyReport(Long userId, int year, int month) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 월의 시작일과 마지막일 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN);
        LocalDateTime endOfMonth = LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.MAX);

        // 해당 월의 일기 조회
        List<Diary> monthlyDiaries = diaryRepository.findByUserUserIdAndCreatedAtBetween(userId, startOfMonth, endOfMonth);

        if (monthlyDiaries.isEmpty()) {
            throw new ResourceNotFoundException("해당 월의 일기가 존재하지 않습니다.");
        }

        try {
            // 일기를 일별로 그룹화 (Map<일자, Map<일기유형, 일기내용>>)
            Map<LocalDate, Map<QType, String>> diaryEntriesByDate = monthlyDiaries.stream()
                    .collect(Collectors.groupingBy(
                            diary -> diary.getCreatedAt().toLocalDate(),
                            Collectors.toMap(
                                    Diary::getQtype,
                                    Diary::getDiary,
                                    (existing, replacement) -> replacement)
                    ));

            // Gemini API 요청을 위한 일기 목록 생성
            List<Map<String, String>> diaryEntries = prepareDiaryEntries(diaryEntriesByDate);

            log.info("일기 분석 시작: 사용자 ID={}, 연도={}, 월={}, 일기 수={}", userId, year, month, diaryEntries.size());

            // 감정 분석
            log.debug("감정 분석 시작");
            JsonNode emotionAnalysis = analyzeEmotions(diaryEntries);
            log.debug("감정 분석 완료: {}", emotionAnalysis);

            // 위험 분석
            log.debug("위험 분석 시작");
            JsonNode riskAnalysis = analyzeRisks(diaryEntries);
            boolean hasRisk = riskAnalysis.get("has_mental_health_risk").asBoolean();
            log.debug("위험 분석 완료: 위험={}", hasRisk);

            // 체크업 추천 (검색 쿼리 생성)
            log.debug("체크업 추천 생성 시작");
            JsonNode checkupInfo = generateCheckups(hasRisk);
            String searchQuery = checkupInfo.get("search_query").asText();
            log.debug("체크업 추천 생성 완료: 검색 쿼리={}", searchQuery);

            // Google CSE를 사용하여 추천 항목 검색
            log.debug("Google CSE 검색 시작");
            List<MonthlyReportDto.RecommendationDto> recommendations = searchRecommendations(searchQuery, hasRisk);
            log.debug("Google CSE 검색 완료: 추천 항목 수={}", recommendations.size());

            // 개요 요약
            log.debug("개요 요약 생성 시작");
            JsonNode overview = summarizeOverview(
                    emotionAnalysis.get("summary").asText(),
                    riskAnalysis.get("risk_analysis").asText());
            log.debug("개요 요약 생성 완료");

            // MonthlyReportDto 생성
            MonthlyReportDto reportDto = buildMonthlyReportDto(
                    overview,
                    emotionAnalysis,
                    riskAnalysis,
                    searchQuery,
                    hasRisk,
                    recommendations);

            // 기존 리포트가 있으면 삭제
            monthlyReportRepository.findByUserUserIdAndYearAndMonth(userId, year, month)
                    .ifPresent(monthlyReportRepository::delete);

            // DB에 저장
            MonthlyReport reportEntity = saveMonthlyReport(user, year, month, reportDto);
            log.info("월간 리포트 저장 완료: 사용자 ID={}, 연도={}, 월={}, 리포트 ID={}",
                    userId, year, month, reportEntity.getId());

            return reportDto;

        } catch (Exception e) {
            log.error("월간 리포트 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new BadRequestException("월간 리포트를 생성하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 월간 리포트를 DB에 저장합니다.
     */
    @Transactional
    protected MonthlyReport saveMonthlyReport(User user, int year, int month, MonthlyReportDto reportDto) {
        // 감정 키워드 추출
        List<String> emotionKeywords = reportDto.getEmotionKeywords();

        // 추천 항목 변환
        List<MonthlyReport.Recommendation> recommendations = reportDto.getRecommendations().stream()
                .map(rec -> new MonthlyReport.Recommendation(rec.getTitle(), rec.getLink()))
                .collect(Collectors.toList());

        // 새 리포트 생성
        MonthlyReport report = MonthlyReport.builder()
                .user(user)
                .year(year)
                .month(month)
                .oneLineSummary(reportDto.getOneLineSummary())
                .overview(reportDto.getOverview())
                .emotionSummary(reportDto.getEmotionSummary())
                .riskAnalysis(reportDto.getRiskAnalysis())
                .hasMentalHealthRisk(reportDto.isHasMentalHealthRisk())
                .checkupType(reportDto.getCheckupType())
                .recommendationMessage(reportDto.getRecommendationMessage())
                .searchQuery(reportDto.getSearchQuery())
                .emotionKeywords(new ArrayList<>(emotionKeywords))
                .recommendations(recommendations)
                .build();

        return monthlyReportRepository.save(report);
    }

    /**
     * 엔티티를 DTO로 변환합니다.
     */
    private MonthlyReportDto convertToDto(MonthlyReport report) {
        // 추천 항목 변환
        List<MonthlyReportDto.RecommendationDto> recommendations = report.getRecommendations().stream()
                .map(rec -> new MonthlyReportDto.RecommendationDto(rec.getTitle(), rec.getLink()))
                .collect(Collectors.toList());

        return MonthlyReportDto.builder()
                .oneLineSummary(report.getOneLineSummary())
                .overview(report.getOverview())
                .emotionKeywords(report.getEmotionKeywords())
                .emotionSummary(report.getEmotionSummary())
                .riskAnalysis(report.getRiskAnalysis())
                .hasMentalHealthRisk(report.isHasMentalHealthRisk())
                .checkupType(report.getCheckupType())
                .recommendationMessage(report.getRecommendationMessage())
                .searchQuery(report.getSearchQuery())
                .recommendations(recommendations)
                .build();
    }

    /**
     * 일기 데이터를 API 요청용 형식으로 변환합니다.
     */
    private List<Map<String, String>> prepareDiaryEntries(Map<LocalDate, Map<QType, String>> diaryEntriesByDate) {
        List<Map<String, String>> entries = new ArrayList<>();

        // 날짜별로 정렬
        List<LocalDate> sortedDates = new ArrayList<>(diaryEntriesByDate.keySet());
        Collections.sort(sortedDates);

        // 각 날짜별로 일기 항목 생성
        for (LocalDate date : sortedDates) {
            Map<QType, String> dailyEntries = diaryEntriesByDate.get(date);
            Map<String, String> entry = new HashMap<>();

            // QType을 영어로 변환하여 매핑
            if (dailyEntries.containsKey(QType.morning)) {
                entry.put("morning", dailyEntries.get(QType.morning));
            }
            if (dailyEntries.containsKey(QType.lunch)) {
                entry.put("afternoon", dailyEntries.get(QType.lunch));
            }
            if (dailyEntries.containsKey(QType.evening)) {
                entry.put("evening", dailyEntries.get(QType.evening));
            }

            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * 감정 분석을 수행합니다.
     */
    private JsonNode analyzeEmotions(List<Map<String, String>> diaryEntries) throws JsonProcessingException {
        String prompt = createPromptForEmotionAnalysis(diaryEntries);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "analyze_emotions");
    }

    /**
     * 정신 건강 위험 분석을 수행합니다.
     */
    private JsonNode analyzeRisks(List<Map<String, String>> diaryEntries) throws JsonProcessingException {
        String prompt = createPromptForRiskAnalysis(diaryEntries);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "analyze_risks");
    }

    /**
     * 체크업 추천을 생성합니다.
     */
    private JsonNode generateCheckups(boolean riskFlag) throws JsonProcessingException {
        String prompt = createPromptForCheckups(riskFlag);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "generate_checkups");
    }

    /**
     * 개요 요약을 생성합니다.
     */
    private JsonNode summarizeOverview(String emotionSummary, String riskAnalysis) throws JsonProcessingException {
        String prompt = createPromptForOverview(emotionSummary, riskAnalysis);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "summarize_overview");
    }

    /**
     * Google Custom Search API를 사용하여 추천 항목을 검색합니다.
     */
    private List<MonthlyReportDto.RecommendationDto> searchRecommendations(String searchQuery, boolean hasRisk) {
        List<MonthlyReportDto.RecommendationDto> recommendations = new ArrayList<>();

        try {
            // Google Custom Search API URL
            String cseUrl = "https://www.googleapis.com/customsearch/v1";

            // 요청 매개변수 설정
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cseUrl)
                    .queryParam("key", googleCseApiKey)
                    .queryParam("cx", googleCseEngineId)
                    .queryParam("q", searchQuery)
                    .queryParam("num", 3); // 최대 3개 결과 요청

            // API 호출
            ResponseEntity<String> response = restTemplate.getForEntity(
                    builder.toUriString(),
                    String.class
            );

            // 응답 처리
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode items = rootNode.path("items");

                if (items.isArray() && items.size() > 0) {
                    for (JsonNode item : items) {
                        String title = item.path("title").asText();
                        String link = item.path("link").asText();

                        // 유효한 제목과 링크인 경우만 추가
                        if (!title.isEmpty() && !link.isEmpty()) {
                            recommendations.add(new MonthlyReportDto.RecommendationDto(title, link));
                        }
                    }

                    log.info("Google CSE 검색 성공: 쿼리='{}', 결과 수={}", searchQuery, recommendations.size());
                    return recommendations;
                } else {
                    log.warn("Google CSE 검색 결과 없음: 쿼리='{}'", searchQuery);
                }
            } else {
                log.warn("Google CSE API 호출 실패: 상태 코드={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Google CSE API 호출 중 오류 발생: {}", e.getMessage(), e);
        }

        // API 호출 실패 또는 결과가 없는 경우 기본 추천 항목 제공
        log.info("기본 추천 항목 사용");
        return getDefaultRecommendations(hasRisk);
    }

    /**
     * 기본 추천 항목을 반환합니다.
     */
    private List<MonthlyReportDto.RecommendationDto> getDefaultRecommendations(boolean hasRisk) {
        List<MonthlyReportDto.RecommendationDto> recommendations = new ArrayList<>();

        if (hasRisk) {
            // 정신 건강 위험이 있는 경우
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "Depression Self-Assessment Test",
                    "https://www.mentalhealth.go.kr/self/selfTest.do"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "Stress Self-Diagnosis Test",
                    "https://health.kdca.go.kr/health/pvsnMental/stress/stress.jsp"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "Anxiety Disorder Self-Assessment",
                    "https://www.nhis.or.kr/nhis/healthin/wbhea0405m01.do"));
        } else {
            // 정신 건강 위험이 없는 경우
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "MBTI Personality Type Test - 16Personalities",
                    "https://www.16personalities.com"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "Character Strengths Test - VIA Institute",
                    "https://www.viacharacter.org/"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "Enneagram Personality Type Test",
                    "https://www.enneagraminstitute.com/"));
        }

        return recommendations;
    }

    /**
     * 감정 분석을 위한 프롬프트를 생성합니다.
     */
    private String createPromptForEmotionAnalysis(List<Map<String, String>> diaryEntries) {
        String fullText = createFullText(diaryEntries);

        return "You are an AI emotion analyst.\n\n" +
                "Analyze the following diary entries written in Korean. These entries include morning, lunch, and dinner reflections each day, written by a single user.\n\n" +
                "Your task is to summarize the emotional flow of the user throughout the month. Focus on:\n" +
                "- Frequently mentioned emotional keywords (e.g., tired, anxious, grateful)\n" +
                "- Changes in tone and emotional state over time (beginning, middle, and end of the month)\n" +
                "- Patterns in emotional fluctuation (e.g., days or times with noticeable mood swings)\n\n" +
                "! When listing emotional keywords:\n" +
                "- Choose **exactly 3 to 5** individual emotions that appeared most frequently.\n" +
                "- **Do not use slashes** (e.g., avoid \"tired/anxious\" — choose only one word per bullet).\n" +
                "- Use simple, distinct emotion words that best represent the user's overall mood trends.\n\n" +
                "Output in English (not Korean):\n" +
                "- A bullet list of exactly 3 to 5 emotion keywords (one per line, no slashes)\n" +
                "- A concise paragraph (200~300 words) summarizing the emotional flow across the month\n\n" +
                "Now here is the diary content:\n" + fullText;
    }

    /**
     * 위험 분석을 위한 프롬프트를 생성합니다.
     */
    private String createPromptForRiskAnalysis(List<Map<String, String>> diaryEntries) {
        String fullText = createFullText(diaryEntries);

        return "You are a mental health analyst AI.\n\n" +
                "Below are emotional diary entries written by a single user in Korean.\n\n" +
                "Your task is to analyze these entries and determine whether the user shows any signs of mental health risks such as:\n" +
                "- persistent low mood or fatigue,\n" +
                "- emotional exhaustion or burnout,\n" +
                "- anxiety, irritability, or self-doubt,\n" +
                "- expressions of hopelessness, isolation, or avoidance.\n\n" +
                "If such risks are detected, write a 200–300 word paragraph in English explaining:\n" +
                "- what emotional signals suggest these risks,\n" +
                "- when they appear in the diary (e.g., early/mid/late month),\n" +
                "- what kind of risk it may indicate (depression, burnout, etc.),\n" +
                "- and a gentle recommendation to take a self-assessment or seek support.\n\n" +
                "If no such risks are found, instead write a 200–300 word encouraging paragraph in English that:\n" +
                "- highlights signs of emotional resilience, reflection, or recovery,\n" +
                "- commends the user for emotional self-awareness and regulation,\n" +
                "- and gently suggests continuing these habits, possibly with a light personality or emotional quiz for insight.\n\n" +
                "After your paragraph, add a final line:\n" +
                "[MENTAL_HEALTH_RISK: YES] or [MENTAL_HEALTH_RISK: NO]\n\n" +
                "Diary:\n" + fullText;
    }

    /**
     * 체크업 추천을 위한 프롬프트를 생성합니다.
     */
    private String createPromptForCheckups(boolean riskFlag) {
        if (riskFlag) {
            return "Suggest a short English search phrase for a mental health self-assessment test (3~6 words). " +
                    "Example: 'depression self assessment test', 'anxiety disorder checklist'";
        } else {
            return "Suggest a short English search phrase for a light personality or emotional quiz (3~6 words). " +
                    "Example: 'personality type test', 'emotional awareness quiz'";
        }
    }

    /**
     * 개요 요약을 위한 프롬프트를 생성합니다.
     */
    private String createPromptForOverview(String emotionSummary, String riskAnalysis) {
        return "You are an AI assistant generating a short monthly summary for a mental health report in English.\n\n" +
                "Below are two summaries:\n" +
                "1. Emotional flow\n" +
                "2. Mental health risk analysis\n\n" +
                "Your task:\n" +
                "- Write a ONE_LINE_SUMMARY (like a title or quote).\n" +
                "- Write a PARAGRAPH_SUMMARY (2-3 sentences, <100 words).\n\n" +
                "Respond in this format:\n" +
                "ONE_LINE_SUMMARY: <summary>\n" +
                "PARAGRAPH_SUMMARY: <paragraph>\n\n" +
                "Emotion Summary:\n" + emotionSummary + "\n\n" +
                "Risk Analysis:\n" + riskAnalysis;
    }

    /**
     * 일기 내용을 텍스트로 변환합니다.
     */
    private String createFullText(List<Map<String, String>> diaryEntries) {
        StringBuilder fullText = new StringBuilder();

        for (int i = 0; i < diaryEntries.size(); i++) {
            Map<String, String> entry = diaryEntries.get(i);
            fullText.append("Day ").append(i + 1).append(":\n");

            if (entry.containsKey("morning")) {
                fullText.append("Morning: ").append(entry.get("morning")).append("\n");
            }
            if (entry.containsKey("afternoon")) {
                fullText.append("Afternoon: ").append(entry.get("afternoon")).append("\n");
            }
            if (entry.containsKey("evening")) {
                fullText.append("Evening: ").append(entry.get("evening")).append("\n");
            }

            fullText.append("\n");
        }

        return fullText.toString();
    }

    /**
     * Gemini API를 호출합니다.
     */
    private String callGeminiApi(String prompt) {
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(geminiUrl)
                    .queryParam("key", geminiApiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            parts.add(textPart);

            content.put("parts", parts);
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Gemini API 호출 성공");
                return response.getBody();
            } else {
                log.warn("Gemini API 호출 실패: 상태 코드={}", response.getStatusCode());
                throw new BadRequestException("Gemini API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new BadRequestException("Gemini API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * API 응답을 파싱합니다.
     */
    private JsonNode parseResponse(String response, String functionName) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode candidates = rootNode.path("candidates");

        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            if (content.has("parts") && content.get("parts").isArray() && content.get("parts").size() > 0) {
                String text = content.get("parts").get(0).path("text").asText();

                // 함수명에 따라 다른 파싱 로직 적용
                switch (functionName) {
                    case "analyze_emotions":
                        return parseEmotionAnalysis(text);
                    case "analyze_risks":
                        return parseRiskAnalysis(text);
                    case "generate_checkups":
                        return parseCheckups(text);
                    case "summarize_overview":
                        return parseOverview(text);
                    default:
                        throw new BadRequestException("지원하지 않는 함수명: " + functionName);
                }
            }
        }

        log.warn("Gemini API 응답 형식 오류: {}", response);
        throw new BadRequestException("API 응답을 파싱할 수 없습니다.");
    }

    /**
     * 감정 분석 결과를 파싱합니다.
     */
    private JsonNode parseEmotionAnalysis(String text) throws JsonProcessingException {
        List<String> keywords = new ArrayList<>();
        StringBuilder summary = new StringBuilder();
        boolean inKeywords = true;

        // 줄별로 파싱
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 글머리 기호로 시작하는 줄은 감정 키워드로 처리
            if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                if (inKeywords) {
                    String keyword = line.replaceFirst("^[-*•]\\s*", "").trim();
                    keywords.add(keyword);
                }
            } else {
                // 글머리 기호 없는 줄은 요약으로 처리
                inKeywords = false;
                if (summary.length() > 0) summary.append(" ");
                summary.append(line);
            }
        }

        // 키워드가 3-5개가 아닌 경우 로그 출력
        if (keywords.size() < 3 || keywords.size() > 5) {
            log.warn("감정 키워드 수가 예상과 다릅니다: {}", keywords.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keywords", keywords);
        result.put("summary", summary.toString());

        return objectMapper.valueToTree(result);
    }

    /**
     * 위험 분석 결과를 파싱합니다.
     */
    private JsonNode parseRiskAnalysis(String text) throws JsonProcessingException {
        boolean hasRisk = text.contains("[MENTAL_HEALTH_RISK: YES]");
        String analysis = text
                .replace("[MENTAL_HEALTH_RISK: YES]", "")
                .replace("[MENTAL_HEALTH_RISK: NO]", "")
                .trim();

        Map<String, Object> result = new HashMap<>();
        result.put("risk_analysis", analysis);
        result.put("has_mental_health_risk", hasRisk);

        return objectMapper.valueToTree(result);
    }

    /**
     * 체크업 추천 결과를 파싱합니다.
     */
    private JsonNode parseCheckups(String text) throws JsonProcessingException {
        Map<String, Object> result = new HashMap<>();
        result.put("search_query", text.trim());

        return objectMapper.valueToTree(result);
    }

    /**
     * 개요 요약 결과를 파싱합니다.
     */
    private JsonNode parseOverview(String text) throws JsonProcessingException {
        String oneLineSummary = "";
        StringBuilder paragraphSummary = new StringBuilder();

        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("ONE_LINE_SUMMARY:")) {
                oneLineSummary = line.replace("ONE_LINE_SUMMARY:", "").trim();
            } else if (line.startsWith("PARAGRAPH_SUMMARY:")) {
                String paragraph = line.replace("PARAGRAPH_SUMMARY:", "").trim();
                paragraphSummary.append(paragraph);
            } else if (paragraphSummary.length() > 0) {
                paragraphSummary.append(" ").append(line);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("one_line_summary", oneLineSummary);
        result.put("overview", paragraphSummary.toString());

        return objectMapper.valueToTree(result);
    }

    /**
     * MonthlyReportDto를 구성합니다.
     */
    private MonthlyReportDto buildMonthlyReportDto(
            JsonNode overview,
            JsonNode emotionAnalysis,
            JsonNode riskAnalysis,
            String searchQuery,
            boolean hasRisk,
            List<MonthlyReportDto.RecommendationDto> recommendations) {

        // 감정 키워드 목록 추출
        List<String> emotionKeywords = new ArrayList<>();
        if (emotionAnalysis.has("keywords") && emotionAnalysis.get("keywords").isArray()) {
            for (JsonNode keyword : emotionAnalysis.get("keywords")) {
                emotionKeywords.add(keyword.asText());
            }
        }

        // 체크업 유형 및 메시지 설정
        String checkupType = hasRisk ? "mental_health_checkup" : "light_personality_quiz";
        String recommendationMessage = hasRisk
                ? "Check your mental health with these self-assessment tools:"
                : "Learn more about yourself with these personality quizzes:";

        return MonthlyReportDto.builder()
                .oneLineSummary(overview.get("one_line_summary").asText())
                .overview(overview.get("overview").asText())
                .emotionKeywords(emotionKeywords)
                .emotionSummary(emotionAnalysis.get("summary").asText())
                .riskAnalysis(riskAnalysis.get("risk_analysis").asText())
                .hasMentalHealthRisk(hasRisk)
                .checkupType(checkupType)
                .recommendationMessage(recommendationMessage)
                .searchQuery(searchQuery)
                .recommendations(recommendations)
                .build();
    }
}
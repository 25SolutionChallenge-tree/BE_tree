package asia.canopy.tree.service;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.QType;
import asia.canopy.tree.dto.MonthlyReportDto;
import asia.canopy.tree.exception.BadRequestException;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.DiaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    /**
     * 특정 월의 일기를 분석하여 월간 리포트를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param year   연도
     * @param month  월
     * @return 월간 리포트 DTO
     */
    public MonthlyReportDto generateMonthlyReport(Long userId, int year, int month) {
        // 월의 시작일과 마지막일 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN);
        LocalDateTime endOfMonth = LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.MAX);

        // 해당 월의 일기 조회
        List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndCreatedAtBetween(userId, startOfMonth, endOfMonth);

        if (monthlyDiaries.isEmpty()) {
            throw new ResourceNotFoundException("해당 월의 일기가 존재하지 않습니다.");
        }

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

        try {
            // 감정 분석
            JsonNode emotionAnalysis = analyzeEmotions(diaryEntries);

            // 위험 분석
            JsonNode riskAnalysis = analyzeRisks(diaryEntries);

            boolean hasRisk = riskAnalysis.get("has_mental_health_risk").asBoolean();

            // 체크업 추천
            JsonNode checkups = generateCheckups(hasRisk);

            // 개요 요약
            JsonNode overview = summarizeOverview(
                    emotionAnalysis.get("summary").asText(),
                    riskAnalysis.get("risk_analysis").asText());

            // MonthlyReportDto 생성 및 반환
            return buildMonthlyReportDto(
                    overview,
                    emotionAnalysis,
                    riskAnalysis,
                    checkups);
        } catch (Exception e) {
            log.error("월간 리포트 생성 중 오류 발생", e);
            throw new BadRequestException("월간 리포트를 생성하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private List<Map<String, String>> prepareDiaryEntries(Map<LocalDate, Map<QType, String>> diaryEntriesByDate) {
        List<Map<String, String>> entries = new ArrayList<>();

        // 각 날짜별로 일기 항목 생성
        for (Map.Entry<LocalDate, Map<QType, String>> dateEntry : diaryEntriesByDate.entrySet()) {
            Map<String, String> entry = new HashMap<>();

            // QType을 영어로 변환하여 매핑
            Map<QType, String> dailyEntries = dateEntry.getValue();
            if (dailyEntries.containsKey(QType.morning)) {
                entry.put("morning", dailyEntries.get(QType.morning));
            }
            if (dailyEntries.containsKey(QType.lunch)) {
                entry.put("afternoon", dailyEntries.get(QType.lunch));
            }
            if (dailyEntries.containsKey(QType.dinner)) {
                entry.put("evening", dailyEntries.get(QType.dinner));
            }

            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private JsonNode analyzeEmotions(List<Map<String, String>> diaryEntries) throws JsonProcessingException {
        String prompt = createPromptForEmotionAnalysis(diaryEntries);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "analyze_emotions");
    }

    private JsonNode analyzeRisks(List<Map<String, String>> diaryEntries) throws JsonProcessingException {
        String prompt = createPromptForRiskAnalysis(diaryEntries);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "analyze_risks");
    }

    private JsonNode generateCheckups(boolean riskFlag) throws JsonProcessingException {
        String prompt = createPromptForCheckups(riskFlag);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "generate_checkups");
    }

    private JsonNode summarizeOverview(String emotionSummary, String riskAnalysis) throws JsonProcessingException {
        String prompt = createPromptForOverview(emotionSummary, riskAnalysis);
        String response = callGeminiApi(prompt);
        return parseResponse(response, "summarize_overview");
    }

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
                "Output in Korean:\n" +
                "- A bullet list of exactly 3 to 5 emotion keywords (one per line, no slashes)\n" +
                "- A concise paragraph (200~300 words) summarizing the emotional flow across the month\n\n" +
                "Now here is the diary content:\n" + fullText;
    }

    private String createPromptForRiskAnalysis(List<Map<String, String>> diaryEntries) {
        String fullText = createFullText(diaryEntries);

        return "You are a mental health analyst AI.\n\n" +
                "Below are emotional diary entries written by a single user in Korean.\n\n" +
                "Your task is to analyze these entries and determine whether the user shows any signs of mental health risks such as:\n" +
                "- persistent low mood or fatigue,\n" +
                "- emotional exhaustion or burnout,\n" +
                "- anxiety, irritability, or self-doubt,\n" +
                "- expressions of hopelessness, isolation, or avoidance.\n\n" +
                "If such risks are detected, write a 200–300 word paragraph in Korean explaining:\n" +
                "- what emotional signals suggest these risks,\n" +
                "- when they appear in the diary (e.g., early/mid/late month),\n" +
                "- what kind of risk it may indicate (depression, burnout, etc.),\n" +
                "- and a gentle recommendation to take a self-assessment or seek support.\n\n" +
                "If no such risks are found, instead write a 200–300 word encouraging paragraph in Korean that:\n" +
                "- highlights signs of emotional resilience, reflection, or recovery,\n" +
                "- commends the user for emotional self-awareness and regulation,\n" +
                "- and gently suggests continuing these habits, possibly with a light personality or emotional quiz for insight.\n\n" +
                "After your paragraph, add a final line:\n" +
                "[MENTAL_HEALTH_RISK: YES] or [MENTAL_HEALTH_RISK: NO]\n\n" +
                "Diary:\n" + fullText;
    }

    private String createPromptForCheckups(boolean riskFlag) {
        String prompt;

        if (riskFlag) {
            prompt = "Suggest a short Korean search phrase for a mental health self-assessment test (3~6 words). " +
                    "Example: '우울증 자가 진단 테스트', '불안장애 체크리스트'";
        } else {
            prompt = "Suggest a short Korean search phrase for a light personality or emotional quiz (3~6 words). " +
                    "Example: '성격유형 테스트', '감정 인식 퀴즈'";
        }

        return prompt;
    }

    private String createPromptForOverview(String emotionSummary, String riskAnalysis) {
        return "You are an AI assistant generating a short monthly summary for a mental health report in Korean.\n\n" +
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

    private String callGeminiApi(String prompt) {
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

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

        return response.getBody();
    }

    private JsonNode parseResponse(String response, String functionName) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode candidates = rootNode.path("candidates");

        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            if (content.has("parts") && content.get("parts").isArray() && content.get("parts").size() > 0) {
                String text = content.get("parts").get(0).path("text").asText();

                // 함수명에 따라 다른 파싱 로직 적용
                if ("analyze_emotions".equals(functionName)) {
                    return parseEmotionAnalysis(text);
                } else if ("analyze_risks".equals(functionName)) {
                    return parseRiskAnalysis(text);
                } else if ("generate_checkups".equals(functionName)) {
                    return parseCheckups(text);
                } else if ("summarize_overview".equals(functionName)) {
                    return parseOverview(text);
                }
            }
        }

        throw new BadRequestException("Gemini API 응답을 파싱하는 중 오류가 발생했습니다.");
    }

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

        Map<String, Object> result = new HashMap<>();
        result.put("keywords", keywords);
        result.put("summary", summary.toString());

        return objectMapper.valueToTree(result);
    }

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

    private JsonNode parseCheckups(String text) throws JsonProcessingException {
        // 여기서는 간단히 검색 쿼리만 반환
        Map<String, Object> result = new HashMap<>();
        result.put("search_query", text.trim());
        result.put("recommendations", new ArrayList<>());

        return objectMapper.valueToTree(result);
    }

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

    private MonthlyReportDto buildMonthlyReportDto(
            JsonNode overview,
            JsonNode emotionAnalysis,
            JsonNode riskAnalysis,
            JsonNode checkups) {

        // 감정 키워드 목록 추출
        List<String> emotionKeywords = new ArrayList<>();
        if (emotionAnalysis.has("keywords") && emotionAnalysis.get("keywords").isArray()) {
            for (JsonNode keyword : emotionAnalysis.get("keywords")) {
                emotionKeywords.add(keyword.asText());
            }
        }

        // 추천 목록 구성 (실제로는 Google 검색 API를 사용하여 채워야 함)
        List<MonthlyReportDto.RecommendationDto> recommendations = new ArrayList<>();

        boolean hasRisk = riskAnalysis.get("has_mental_health_risk").asBoolean();
        String checkupType = hasRisk ? "mental_health_checkup" : "light_personality_quiz";
        String recommendationMessage = hasRisk
                ? "다음 자가진단 도구를 통해 정신 건강을 체크해보세요:"
                : "다음 성격 퀴즈를 통해 자신에 대해 더 알아보세요:";

        // 검색 쿼리 시뮬레이션
        String searchQuery = checkups.has("search_query") ? checkups.get("search_query").asText() : "";

        // 예시 추천 항목 추가
        if (hasRisk) {
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "우울증 자가진단 테스트 - 보건복지부",
                    "https://www.mentalhealth.go.kr/self/selfTest.do"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "스트레스 자가진단 - 국가건강정보포털",
                    "https://health.kdca.go.kr/health/pvsnMental/stress/stress.jsp"));
        } else {
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "MBTI 성격유형 검사 - 16Personalities",
                    "https://www.16personalities.com/ko"));
            recommendations.add(new MonthlyReportDto.RecommendationDto(
                    "성격강점 검사 - VIA Institute",
                    "https://www.viacharacter.org/"));
        }

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
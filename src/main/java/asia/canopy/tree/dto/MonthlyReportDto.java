package asia.canopy.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "월간 일기 분석 결과 DTO")
public class MonthlyReportDto {

    @Schema(description = "한 줄 요약", example = "긍정적인 성장과 자기 인식의 달")
    private String oneLineSummary;

    @Schema(description = "전체 개요", example = "이번 달은 초반의 스트레스에서 시작해 점차 안정을 찾아가는 여정이었습니다. 자기 성찰과 소소한 일상의 기쁨을 발견하는 과정이 두드러졌습니다.")
    private String overview;

    @Schema(description = "감정 키워드 목록")
    private List<String> emotionKeywords;

    @Schema(description = "감정 흐름 요약")
    private String emotionSummary;

    @Schema(description = "정신 건강 위험 분석")
    private String riskAnalysis;

    @Schema(description = "정신 건강 위험 존재 여부", example = "false")
    private boolean hasMentalHealthRisk;

    @Schema(description = "추천 체크업 유형", example = "light_personality_quiz")
    private String checkupType;

    @Schema(description = "추천 메시지", example = "다음 성격 퀴즈를 통해 자신에 대해 더 알아보세요:")
    private String recommendationMessage;

    @Schema(description = "검색 쿼리", example = "personality type quiz")
    private String searchQuery;

    @Schema(description = "추천 리스트")
    private List<RecommendationDto> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "월간 리포트 추천 항목")
    public static class RecommendationDto {
        @Schema(description = "추천 제목", example = "16Personalities - 무료 성격 테스트")
        private String title;

        @Schema(description = "추천 링크", example = "https://www.16personalities.com/ko")
        private String link;
    }
}
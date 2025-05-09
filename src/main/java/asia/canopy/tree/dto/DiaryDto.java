package asia.canopy.tree.dto;

import asia.canopy.tree.domain.QType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "일기 DTO")
public class DiaryDto {

    @Schema(description = "일기 ID", example = "1")
    private Long diaryId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @NotNull(message = "일기 타입은 필수 입력값입니다.")
    @Schema(description = "일기 타입 (morning, lunch, dinner)", example = "morning")
    private QType qtype;

    @NotNull(message = "일기 내용은 필수 입력값입니다.")
    @Size(max = 1000, message = "일기 내용은 최대 1000자까지 입력 가능합니다.")
    @Schema(description = "일기 내용", example = "오늘의 일기 내용입니다.")
    private String diary;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    // 일기 생성을 위한 DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 생성 DTO")
    public static class CreateRequest {
        @NotNull(message = "일기 타입은 필수 입력값입니다.")
        @Schema(description = "일기 타입 (morning, lunch, dinner)", example = "morning")
        private QType qtype;

        @NotNull(message = "일기 내용은 필수 입력값입니다.")
        @Size(max = 1000, message = "일기 내용은 최대 1000자까지 입력 가능합니다.")
        @Schema(description = "일기 내용", example = "오늘의 일기 내용입니다.")
        private String diary;
    }
}
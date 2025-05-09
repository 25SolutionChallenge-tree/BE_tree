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
@Schema(description = "일기 목록 응답 DTO")
public class DiaryListResponse {

    @Schema(description = "조회된 일기 목록")
    private List<DiaryDto> diaries;

    @Schema(description = "조회된 일기 수", example = "5")
    private int count;
}

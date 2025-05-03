package asia.canopy.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "API Response Data Transfer Object")
public class ApiResponseDto {

    @Schema(description = "Success status", example = "true")
    private boolean success;

    @Schema(description = "Message", example = "Operation completed successfully")
    private String message;
}

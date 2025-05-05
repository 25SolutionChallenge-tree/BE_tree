package asia.canopy.tree.dto;

import asia.canopy.tree.domain.Avatar;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Profile Setup Data Transfer Object")
public class ProfileSetupDto {

    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 30, message = "Nickname must be between 2 and 30 characters")
    @Schema(description = "User's nickname", example = "cooluser123")
    private String nickname;

    @NotNull(message = "Avatar is required")
    @Schema(description = "User's avatar", example = "GREEN", allowableValues = {"GREEN", "YELLOW", "PINK"})
    private Avatar avatar;
}

package asia.canopy.tree.dto;

import asia.canopy.tree.domain.Avatar;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "프로필 업데이트 요청")
public class ProfileUpdateRequest {

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Schema(description = "사용자 닉네임", example = "canopy")
    private String nickname;

    @NotNull(message = "아바타는 필수 입력값입니다.")
    @Schema(description = "사용자 아바타 타입 (GREEN, PINK, YELLOW)", example = "GREEN")
    private Avatar avatar;
}
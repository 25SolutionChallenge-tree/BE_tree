package asia.canopy.tree.dto;

import asia.canopy.tree.domain.Avatar;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 프로필 응답")
public class UserProfileResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "사용자 닉네임", example = "canopy")
    private String nickname;

    @Schema(description = "사용자 아바타", example = "GREEN")
    private Avatar avatar;

    @Schema(description = "이메일 인증 여부", example = "true")
    private boolean emailVerified;

    @Schema(description = "프로필 완성 여부", example = "true")
    private boolean profileComplete;
}
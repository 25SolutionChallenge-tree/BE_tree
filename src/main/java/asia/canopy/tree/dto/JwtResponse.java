package asia.canopy.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT 인증 응답")
public class JwtResponse {
    @Schema(description = "인증 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "토큰 타입", example = "Bearer")
    @Builder.Default
    private String type = "Bearer";

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "프로필 완성 여부", example = "false")
    private boolean profileComplete;

    public JwtResponse(String token, Long id, String email, boolean profileComplete) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.profileComplete = profileComplete;
    }
}
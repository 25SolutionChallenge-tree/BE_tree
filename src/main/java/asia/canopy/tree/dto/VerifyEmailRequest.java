package asia.canopy.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 인증 요청")
public class VerifyEmailRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @NotBlank(message = "인증 코드는 필수 입력값입니다.")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
    @Schema(description = "6자리 이메일 인증 코드", example = "123456")
    private String code;
}
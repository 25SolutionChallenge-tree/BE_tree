package asia.canopy.tree.controller;

import asia.canopy.tree.dto.EmailVerifiedSignUpRequest;
import asia.canopy.tree.dto.JwtResponse;
import asia.canopy.tree.dto.LoginRequest;
import asia.canopy.tree.dto.VerifyEmailRequest;
import asia.canopy.tree.service.AuthService;
import asia.canopy.tree.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 이메일 인증 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Operation(summary = "이메일 유효성 검사", description = "이메일이 유효하고 가입되지 않았는지 확인하고 인증 이메일을 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 이메일 발송 성공"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        if (authService.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 가입된 이메일입니다.");
        }

        emailService.sendVerificationEmail(email);
        return ResponseEntity.ok().body("인증 이메일이 발송되었습니다.");
    }

    @Operation(summary = "인증된 이메일로 회원가입", description = "이메일 인증이 완료된 후 비밀번호를 입력하여 회원가입을 완료합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 인증 코드",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })

    @PostMapping("/verified-signup")
    public ResponseEntity<?> registerVerifiedUser(@Valid @RequestBody EmailVerifiedSignUpRequest signUpRequest) {

        authService.registerVerifiedUser(signUpRequest);

        return ResponseEntity.ok().body("회원 가입이 완료되었습니다.");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }


    @Operation(summary = "이메일 인증 코드 확인", description = "이메일로 전송된 인증 코드를 검증합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증 코드가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailRequest request) {
        boolean verified = authService.verifyEmailForSignUp(request.getEmail(), request.getCode());
        if (verified) {
            return ResponseEntity.ok().body("이메일 인증이 완료되었습니다. 회원가입을 계속 진행해주세요.");
        } else {
            return ResponseEntity.badRequest().body("인증 코드가 올바르지 않습니다.");
        }
    }

    @Operation(summary = "인증 이메일 재전송", description = "이메일 인증 코드를 재전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 이메일 재전송 성공")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        emailService.sendVerificationEmail(email);
        return ResponseEntity.ok().body("인증 이메일이 재전송되었습니다.");
    }
}
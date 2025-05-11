package asia.canopy.tree.controller;

import asia.canopy.tree.config.UserPrincipal;
import asia.canopy.tree.dto.ProfileUpdateRequest;
import asia.canopy.tree.dto.UserProfileResponse;
import asia.canopy.tree.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 프로필 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserProfileResponse userProfile = userService.getUserProfile(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(userProfile);
    }

    @Operation(summary = "사용자 프로필 업데이트", description = "사용자의 닉네임과 아바타를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ProfileUpdateRequest profileUpdateRequest) {
        UserProfileResponse userProfile = userService.updateProfile(
                userPrincipal.getId(),
                profileUpdateRequest.getNickname(),
                profileUpdateRequest.getAvatar()
        );
        return ResponseEntity.ok(userProfile);
    }
}
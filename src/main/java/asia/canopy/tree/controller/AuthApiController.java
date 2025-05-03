package asia.canopy.tree.controller;

import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.*;
import asia.canopy.tree.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API")
public class AuthApiController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationDto registrationDto) {
        try {
            User user = userService.registerUser(registrationDto.getEmail(), registrationDto.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto(true, "User registered successfully. Please check your email for verification."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponseDto(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto(false, "Failed to register user: " + e.getMessage()));
        }
    }

    @Operation(summary = "Verify email", description = "Verifies user's email using the token sent to their email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean verified = userService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(new ApiResponseDto(true, "Email verified successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDto(false, "Invalid or expired verification token"));
        }
    }

    @Operation(summary = "Set password", description = "Sets password for a verified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password set successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@Valid @RequestBody PasswordDto passwordDto) {
        try {
            userService.setPassword(passwordDto.getEmail(), passwordDto.getPassword());
            return ResponseEntity.ok(new ApiResponseDto(true, "Password set successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDto(false, e.getMessage()));
        }
    }

    @Operation(summary = "Login", description = "Login with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        // This is handled by Spring Security, but we include it in the Swagger docs
        // The actual implementation will be in the Spring Security filters
        return ResponseEntity.ok(new LoginResponseDto("token", "User logged in successfully"));
    }
}
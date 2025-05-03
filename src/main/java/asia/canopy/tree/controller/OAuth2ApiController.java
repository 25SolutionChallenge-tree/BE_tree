package asia.canopy.tree.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/oauth2")
@Tag(name = "Social Login", description = "Social Login API")
public class OAuth2ApiController {

    @Operation(summary = "Login with Google",
            description = "Redirects to Google OAuth for authentication. This is for Swagger documentation only. " +
                    "The actual endpoint is handled by Spring Security OAuth2.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Google OAuth")
    })
    @GetMapping("/google")
    public void loginWithGoogle() {
        // This is just for Swagger documentation
        // The actual endpoint is handled by Spring Security OAuth2
    }

    @Operation(summary = "Login with Facebook",
            description = "Redirects to Facebook OAuth for authentication. This is for Swagger documentation only. " +
                    "The actual endpoint is handled by Spring Security OAuth2.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Facebook OAuth")
    })
    @GetMapping("/facebook")
    public void loginWithFacebook() {
        // This is just for Swagger documentation
        // The actual endpoint is handled by Spring Security OAuth2
    }

    @Operation(summary = "OAuth2 Callback",
            description = "Callback endpoint for OAuth2 providers. This is for Swagger documentation only. " +
                    "The actual endpoint is handled by Spring Security OAuth2.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to application after successful login")
    })
    @GetMapping("/callback")
    public void oauth2Callback() {
        // This is just for Swagger documentation
        // The actual endpoint is handled by Spring Security OAuth2
    }
}

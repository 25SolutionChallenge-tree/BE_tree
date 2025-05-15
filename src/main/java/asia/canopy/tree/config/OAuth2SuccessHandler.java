package asia.canopy.tree.config;

import asia.canopy.tree.config.jwt.JwtTokenProvider;
import asia.canopy.tree.exception.OAuth2AuthenticationProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        UserPrincipal userPrincipal;

        if (oAuth2User instanceof UserPrincipal) {
            userPrincipal = (UserPrincipal) oAuth2User;
        } else {
            throw new OAuth2AuthenticationProcessingException("인증 정보를 처리할 수 없습니다.");
        }

        String token = jwtTokenProvider.generateToken(userPrincipal);
        String redirectUrl = UriComponentsBuilder.fromUriString("https://www.canopy.asia/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("isProfileComplete", userPrincipal.getUser().isProfileComplete())
                .build().toUriString();

        if (!userPrincipal.getUser().isProfileComplete()) {
            redirectUrl = UriComponentsBuilder.fromUriString("https://www.canopy.asia/select-character")
                    .queryParam("token", token)
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
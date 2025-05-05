package asia.canopy.tree.config;

import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/register/**", "/verify/**", "/login/**",
                                "/set-password", "/css/**", "/js/**",
                                "/api/auth/register", "/api/auth/verify",
                                "/api/auth/set-password", "/api/auth/login").permitAll()
                        .requestMatchers("/profile-setup").authenticated()
                        .requestMatchers("/api/auth/profile-setup").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();
                            if (requestUri.startsWith("/api/")) {
                                // API 경로인 경우 JSON 응답 반환
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());

                                // JSON 응답 생성
                                Map<String, Object> responseData = new HashMap<>();
                                responseData.put("success", false);
                                responseData.put("message", "User not authenticated");

                                ObjectMapper mapper = new ObjectMapper();
                                PrintWriter out = response.getWriter();
                                out.print(mapper.writeValueAsString(responseData));
                                out.flush();
                            } else {
                                // 웹 페이지 경로인 경우 로그인 페이지로 리다이렉트
                                response.sendRedirect("/login");
                            }
                        })
                );

        return http.build();
    }

    // 인증 성공 시 프로필 완성 여부 확인
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getEmail()).orElseThrow();

            if (!user.isProfileCompleted()) {
                response.sendRedirect("/profile-setup");
            } else {
                response.sendRedirect("/dashboard");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
                googleClientRegistration(),
                facebookClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("google-client-id")
                .clientSecret("google-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    private ClientRegistration facebookClientRegistration() {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId("facebook-app-id")
                .clientSecret("facebook-app-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v16.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v16.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/v16.0/me?fields=id,name,email,picture")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}
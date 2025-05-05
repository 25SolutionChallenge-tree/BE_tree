package asia.canopy.tree.AuthTest;

import asia.canopy.tree.config.CustomUserDetails;
import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.Avatar;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc  // addFilters=false 제거
@Transactional
@ActiveProfiles("test")
@Import(AuthApiControllerTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // Custom Annotation
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
    public @interface WithMockCustomUser {
        String email() default "test@example.com";
    }

    // SecurityContext Factory
    public static class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            User user = User.builder()
                    .email(annotation.email())
                    .emailVerified(true)
                    .profileCompleted(false)
                    .provider(AuthProvider.LOCAL)
                    .build();

            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    customUserDetails, null, customUserDetails.getAuthorities());

            context.setAuthentication(auth);
            return context;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JavaMailSender javaMailSender() {
            JavaMailSender mockMailSender = mock(JavaMailSender.class);
            MimeMessage mockMimeMessage = mock(MimeMessage.class);
            when(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
            return mockMailSender;
        }
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_Success() throws Exception {
        String requestBody = """
            {
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerUser_EmailAlreadyExists() throws Exception {
        // given
        String email = "existing@example.com";
        User existingUser = User.builder()
                .email(email)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        userRepository.save(existingUser);

        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(email);

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void verifyEmail_ValidToken() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email("test@example.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .profileCompleted(false)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(1))
                .build();

        userRepository.save(user);

        // when & then
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void verifyEmail_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    void setPassword_Success() throws Exception {
        // given
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        userRepository.save(user);

        String requestBody = """
            {
                "email": "%s",
                "password": "password123",
                "confirmPassword": "password123"
            }
            """.formatted(email);

        // when & then
        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password set successfully"));
    }

    @Test
    @WithMockCustomUser(email = "test@example.com")
    void setupProfile_Success() throws Exception {
        // given
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        userRepository.save(user);

        String requestBody = """
            {
                "nickname": "testuser",
                "avatar": "GREEN"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/auth/profile-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile setup completed successfully"));
    }

    @Test
    void setupProfile_Unauthenticated() throws Exception {
        String requestBody = """
            {
                "nickname": "testuser",
                "avatar": "GREEN"
            }
            """;

        mockMvc.perform(post("/api/auth/profile-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }
}
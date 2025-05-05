package asia.canopy.tree.AuthTest;

import asia.canopy.tree.config.CustomUserDetails;
import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.Avatar;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() throws Exception {
        ApplicationContext context = mockMvc.getDispatcherServlet()
                .getWebApplicationContext();

        try {
            Object authController = context.getBean("authController");
            System.out.println("AuthController bean found: " + authController);
        } catch (NoSuchBeanDefinitionException e) {
            System.out.println("AuthController bean not found!");
        }

        // 모든 매핑 출력
        RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
        mapping.getHandlerMethods().forEach((info, method) -> {
            System.out.println(info + " -> " + method);
        });
    }

    @Test
    void showLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void showRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registrationDto"));
    }

    @Test
    void registerUser_Success() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void verifyEmail_ValidToken() throws Exception {
        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email("test@example.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .profileCompleted(false)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(1))
                .build();
        user = userRepository.save(user);

        mockMvc.perform(get("/verify").param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/set-password"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void verifyEmail_InvalidToken() throws Exception {
        mockMvc.perform(get("/verify").param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void showSetPasswordPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("verifiedEmail", true);

        mockMvc.perform(get("/set-password").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("set-password"))
                .andExpect(model().attributeExists("passwordDto"));
    }

    @Test
    void showSetPasswordPage_WithoutVerification() throws Exception {
        mockMvc.perform(get("/set-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void showProfileSetupPage() throws Exception {
        User user = User.builder()
                .email("test@example.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        mockMvc.perform(get("/profile-setup")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-setup"))
                .andExpect(model().attributeExists("profileSetupDto"));
    }

    @Test
    void showProfileSetupPage_AlreadyCompleted() throws Exception {
        User user = User.builder()
                .email("test.completed@example.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(true)
                .nickname("testuser")
                .avatar(Avatar.GREEN)
                .build();
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        mockMvc.perform(get("/profile-setup")
                        .with(authentication(auth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void setupProfile_Success() throws Exception {
        User user = User.builder()
                .email("test.setup@example.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        mockMvc.perform(post("/profile-setup")
                        .with(csrf())
                        .with(authentication(auth))
                        .param("nickname", "testuser")
                        .param("avatar", "GREEN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }
}
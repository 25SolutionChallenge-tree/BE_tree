package asia.canopy.tree.AuthTest;

import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
                        .param("email", "test@example.com")
                        .param("name", "Test User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void verifyEmail_ValidToken() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(1))
                .build();

        user = userRepository.save(user);

        // when & then
        mockMvc.perform(get("/verify")
                        .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/set-password"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void verifyEmail_InvalidToken() throws Exception {
        mockMvc.perform(get("/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void showSetPasswordPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("verifiedEmail", "true");

        mockMvc.perform(get("/set-password")
                        .session(session))
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
}
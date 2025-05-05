package asia.canopy.tree.AuthTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void apiPublicEndpoints_ShouldBeAccessible() throws Exception {
        // API 엔드포인트들도 인증 없이 접근 가능한지 테스트
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed()); // GET은 허용되지 않음

        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().isBadRequest()); // token 파라미터가 없음
    }

    @Test
    void profileSetupEndpoints_ShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/profile-setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // API 프로필 설정은 인증 필요 (401 반환)
        mockMvc.perform(get("/api/auth/profile-setup"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"success\":false,\"message\":\"User not authenticated\"}"));
    }

    @Test
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
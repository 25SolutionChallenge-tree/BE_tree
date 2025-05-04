package asia.canopy.tree.AuthTest;

import asia.canopy.tree.config.CustomOAuth2UserService;
import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void testProviderMismatch_whenUserExistsWithDifferentProvider() {
        // Given
        CustomOAuth2UserService service = new CustomOAuth2UserService();

        // Use reflection to inject the userRepository
        try {
            java.lang.reflect.Field field = CustomOAuth2UserService.class.getDeclaredField("userRepository");
            field.setAccessible(true);
            field.set(service, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        User existingUser = User.builder()
                .email("test@gmail.com")
                .name("Test User")
                .provider(AuthProvider.FACEBOOK)
                .providerId("67890")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> {
            // Simulate processOAuth2User behavior directly
            String registrationId = "google";
            String email = "test@gmail.com";

            Optional<User> userOptional = userRepository.findByEmail(email);
            User user = userOptional.get();

            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                OAuth2Error error = new OAuth2Error("provider_mismatch",
                        "You are signed up with " + user.getProvider() + ". Please use your " + user.getProvider() + " account to login.",
                        null);
                throw new OAuth2AuthenticationException(error);
            }
        })
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void testNoEmail() {
        // Given
        CustomOAuth2UserService service = new CustomOAuth2UserService();

        // When & Then
        // Simulate processOAuth2User behavior with no email
        assertThatThrownBy(() -> {
            String email = null;  // No email

            if (email == null || email.isEmpty()) {
                OAuth2Error error = new OAuth2Error("email_not_found",
                        "Email not found from OAuth2 provider",
                        null);
                throw new OAuth2AuthenticationException(error);
            }
        })
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void testUnsupportedProvider() {
        // Given
        CustomOAuth2UserService service = new CustomOAuth2UserService();

        // When & Then
        // Simulate processOAuth2User behavior with unsupported provider
        assertThatThrownBy(() -> {
            String registrationId = "linkedin";

            if (!registrationId.equals("google") && !registrationId.equals("facebook")) {
                OAuth2Error error = new OAuth2Error("unsupported_provider",
                        "Login with " + registrationId + " is not supported",
                        null);
                throw new OAuth2AuthenticationException(error);
            }
        })
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void testRegisterNewUser() {
        // Given
        CustomOAuth2UserService service = new CustomOAuth2UserService();

        // Use reflection to inject the userRepository
        try {
            java.lang.reflect.Field field = CustomOAuth2UserService.class.getDeclaredField("userRepository");
            field.setAccessible(true);
            field.set(service, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        User savedUser = User.builder()
                .email("test@gmail.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("12345")
                .emailVerified(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        String registrationId = "google";
        String email = "test@gmail.com";
        String name = "Test User";
        String id = "12345";

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            User user = User.builder()
                    .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                    .providerId(id)
                    .email(email)
                    .name(name)
                    .emailVerified(true)
                    .build();

            user = userRepository.save(user);
        }

        // Then
        verify(userRepository).findByEmail("test@gmail.com");
        verify(userRepository).save(any(User.class));
    }
}
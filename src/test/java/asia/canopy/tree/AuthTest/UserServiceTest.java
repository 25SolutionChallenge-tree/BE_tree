package asia.canopy.tree.AuthTest;

import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import asia.canopy.tree.service.UserService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_Success() {
        // given
        String email = "test@example.com";
        String name = "Test User";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        User result = userService.registerUser(email, name);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(result.isEmailVerified()).isFalse();

        verify(userRepository).save(any(User.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        // given
        String email = "existing@example.com";
        String name = "Test User";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(email, name))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyEmail_ValidToken() {
        // given
        String token = "valid-token";
        String email = "test@example.com";

        User user = User.builder()
                .email(email)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(1))
                .emailVerified(false)
                .build();

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        boolean result = userService.verifyEmail(token);

        // then
        assertThat(result).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiry()).isNull();
    }

    @Test
    void verifyEmail_ExpiredToken() {
        // given
        String token = "expired-token";
        String email = "test@example.com";

        User user = User.builder()
                .email(email)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().minusHours(1))
                .emailVerified(false)
                .build();

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));

        // when
        boolean result = userService.verifyEmail(token);

        // then
        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void setPassword_Success() {
        // given
        String email = "test@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encoded-password";

        User user = User.builder()
                .email(email)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        // when
        userService.setPassword(email, rawPassword);

        // then
        assertThat(user.getPassword()).isEqualTo(encodedPassword);
        verify(userRepository).save(user);
    }

    @Test
    void setPassword_UserNotFound() {
        // given
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.setPassword(email, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).save(any(User.class));
    }
}

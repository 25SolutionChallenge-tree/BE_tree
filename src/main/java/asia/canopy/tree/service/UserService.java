package asia.canopy.tree.service;

import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.Avatar;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    // 1단계: 이메일 회원가입
    @Transactional
    public User registerUser(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(email)
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .profileCompleted(false)  // 프로필 미완성 상태
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser = userRepository.save(user);
        sendVerificationEmail(user);
        return savedUser;
    }

    public void sendVerificationEmail(User user) {
        try {
            String verificationUrl = baseUrl + "/verify?token=" + user.getVerificationToken();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setSubject("Email Verification");
            helper.setText(
                    "<html><body>" +
                            "<h2>Hello!</h2>" +
                            "<p>Thank you for registering. Please click on the link below to verify your email address:</p>" +
                            "<p><a href=\"" + verificationUrl + "\">Verify Email</a></p>" +
                            "<p>This link will expire in 24 hours.</p>" +
                            "</body></html>",
                    true
            );

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        } catch (MailException e) {
            throw new RuntimeException("Mail server connection failed", e);
        }
    }

    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getVerificationTokenExpiry().isAfter(LocalDateTime.now())) {
                user.setEmailVerified(true);
                user.setVerificationToken(null);
                user.setVerificationTokenExpiry(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void setPassword(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    // 2단계: 프로필 설정
    @Transactional
    public User completeProfile(String email, String nickname, Avatar avatar) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified");
        }

        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setProfileCompleted(true);

        return userRepository.save(user);
    }
}
package asia.canopy.tree.service;

import asia.canopy.tree.domain.VerificationToken;
import asia.canopy.tree.repository.VerificationTokenRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final VerificationTokenRepository verificationTokenRepository;

    @Async
    @Transactional
    public void sendVerificationEmail(String email) {
        String verificationCode = generateRandomCode();
        createVerificationToken(email, verificationCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Canopy - 이메일 인증 코드");
            helper.setText(buildEmailContent(verificationCode), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 발송 중 오류 발생: {}", e.getMessage());
        }
    }

    @Transactional
    public void createVerificationToken(String email, String token) {
        verificationTokenRepository.deleteByEmail(email);

        VerificationToken verificationToken = VerificationToken.builder()
                .email(email)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        verificationTokenRepository.save(verificationToken);
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private String buildEmailContent(String code) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #4CAF50;'>Welcome to Canopy!</h2>"
                + "<p>Please enter the following 6-digit verification code to complete your email verification:</p>"
                + "<div style='background-color: #f2f2f2; padding: 15px; font-size: 24px; text-align: center; letter-spacing: 8px; font-weight: bold;'>"
                + code
                + "</div>"
                + "<p>This code is valid for 15 minutes.</p>"
                + "<p>Thank you,<br>The Canopy Team</p>"
                + "</div>";
    }
}
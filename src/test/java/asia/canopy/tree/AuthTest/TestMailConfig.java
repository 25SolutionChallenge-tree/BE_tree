package asia.canopy.tree.AuthTest;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    @Profile("test")
    public JavaMailSender testJavaMailSender() {
        JavaMailSender mockSender = Mockito.mock(JavaMailSender.class);

        // createMimeMessage 메서드 모킹
        Session session = Session.getInstance(new Properties());
        MimeMessage mockMessage = Mockito.mock(MimeMessage.class);
        Mockito.when(mockSender.createMimeMessage()).thenReturn(mockMessage);

        // send 메서드 모킹
        Mockito.doNothing().when(mockSender).send(Mockito.any(MimeMessage.class));

        return mockSender;
    }
}
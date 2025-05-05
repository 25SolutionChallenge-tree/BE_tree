package asia.canopy.tree.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Avatar avatar;

    private boolean profileCompleted;  // 프로필 설정 완료 여부

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    private boolean emailVerified;

    private String verificationToken;

    private LocalDateTime verificationTokenExpiry;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
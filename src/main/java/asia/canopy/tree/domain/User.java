package asia.canopy.tree.domain;

import asia.canopy.tree.dto.OAuth2UserInfo;
import jakarta.persistence.*;
import lombok.*;
import asia.canopy.tree.domain.Diary;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Avatar avatar;

    private String provider;

    private String providerId;

    private boolean emailVerified;

    private String verificationCode;

    private LocalDateTime verificationExpiry;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isProfileComplete() {
        return nickname != null && avatar != null;
    }

    public void updateFromOAuth2UserInfo(OAuth2UserInfo oAuth2UserInfo) {
        this.email = oAuth2UserInfo.getEmail();
        this.providerId = oAuth2UserInfo.getId();
    }

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diary> diaries = new ArrayList<>();

    public void addDiary(Diary diary) {
        diaries.add(diary);
        diary.setUser(this);
    }

}
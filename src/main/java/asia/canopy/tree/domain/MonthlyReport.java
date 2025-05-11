package asia.canopy.tree.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monthly_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "one_line_summary", length = 255)
    private String oneLineSummary;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "emotion_summary", columnDefinition = "TEXT")
    private String emotionSummary;

    @Column(name = "risk_analysis", columnDefinition = "TEXT")
    private String riskAnalysis;

    @Column(name = "has_mental_health_risk")
    private boolean hasMentalHealthRisk;

    @Column(name = "checkup_type", length = 50)
    private String checkupType;

    @Column(name = "recommendation_message", length = 255)
    private String recommendationMessage;

    @Column(name = "search_query", length = 100)
    private String searchQuery;

    @ElementCollection
    @CollectionTable(
            name = "monthly_report_emotion_keywords",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @Column(name = "keyword")
    private List<String> emotionKeywords = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "monthly_report_recommendations",
            joinColumns = @JoinColumn(name = "report_id")
    )
    private List<Recommendation> recommendations = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        @Column(name = "title", length = 255)
        private String title;

        @Column(name = "link", length = 255)
        private String link;
    }
}

package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.crisis.CrisisType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Persisted record of a single AI-powered financial crisis analysis.
 *
 * <p>{@code userId}          – Nullable; analysis can be performed by anonymous users.</p>
 * <p>{@code inputMyDataJson} – Masked MyData snapshot sent to the LLM (JSONB column).</p>
 * <p>{@code resultJson}      – Validated LLM response payload (JSONB column).</p>
 * <p>{@code tokensUsed}      – Sum of input + output tokens consumed by the LLM call.</p>
 */
@Entity
@Table(name = "analysis_results")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CrisisType crisisType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String situationDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_mydata_json", columnDefinition = "jsonb")
    private Map<String, Object> inputMyDataJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> resultJson;

    @Column(length = 100)
    private String llmProvider;

    @Column(nullable = false)
    @Builder.Default
    private int tokensUsed = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

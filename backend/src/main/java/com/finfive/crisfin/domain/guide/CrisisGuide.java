package com.finfive.crisfin.domain.guide;

import com.finfive.crisfin.domain.crisis.CrisisType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "crisis_guides")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrisisGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CrisisType crisisType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String coachingPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keyRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> sourceLaws;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public CrisisGuide(CrisisType crisisType,
                       String title,
                       String coachingPrompt,
                       List<String> keyRules,
                       List<String> sourceLaws) {
        this.crisisType = crisisType;
        this.title = title;
        this.coachingPrompt = coachingPrompt;
        this.keyRules = keyRules;
        this.sourceLaws = sourceLaws;
    }
}

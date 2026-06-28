package com.finfive.crisfin.domain.welfare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "welfare_benefits")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Builder
public class WelfareBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalServiceId;

    private String serviceName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String targetDescription;

    @Column(columnDefinition = "TEXT")
    private String selectionCriteria;

    @Column(columnDefinition = "TEXT")
    private String applyMethod;

    private String applyUrl;

    private String ministryName;

    private String contact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> crisisTags;

    @Builder.Default
    private boolean isActive = true;

    private LocalDateTime lastSyncedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ------------------------------------------------------------------ //
    //  Mutation helpers (used by scheduler upsert logic)
    // ------------------------------------------------------------------ //

    public void update(String serviceName,
                       String summary,
                       String targetDescription,
                       String selectionCriteria,
                       String applyMethod,
                       String applyUrl,
                       String ministryName,
                       String contact,
                       List<String> crisisTags,
                       LocalDateTime lastSyncedAt) {
        this.serviceName = serviceName;
        this.summary = summary;
        this.targetDescription = targetDescription;
        this.selectionCriteria = selectionCriteria;
        this.applyMethod = applyMethod;
        this.applyUrl = applyUrl;
        this.ministryName = ministryName;
        this.contact = contact;
        this.crisisTags = crisisTags;
        this.lastSyncedAt = lastSyncedAt;
        this.isActive = true;
    }
}

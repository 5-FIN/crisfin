package com.finfive.crisfin.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A user's access entitlement for the paid analysis feature.
 *
 * <p>One row per user. {@code expiresAt == null} means the entitlement does not expire.
 * {@link #isActive()} encapsulates the "currently usable" check consumed by the paid-gating
 * logic in the analysis flow.</p>
 */
@Entity
@Table(name = "entitlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntitlementStatus status;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 남은 이용 횟수. {@code null}이면 무제한. */
    @Column(name = "remaining_uses")
    private Integer remainingUses;

    /** Whether the entitlement currently grants access. */
    public boolean isActive() {
        return status == EntitlementStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()))
                && (remainingUses == null || remainingUses > 0);
    }

    /** Re-activates (or extends) this entitlement after a successful payment. */
    public void activate(String plan, LocalDateTime expiresAt, Integer remainingUses) {
        this.plan = plan;
        this.status = EntitlementStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.remainingUses = remainingUses;
    }

    /** 유한 이용권일 때 남은 횟수를 1 차감한다. 무제한이면 아무 것도 하지 않는다. */
    public void consumeOneUse() {
        if (remainingUses != null && remainingUses > 0) {
            this.remainingUses -= 1;
        }
    }

    public enum EntitlementStatus {
        ACTIVE, INACTIVE
    }
}

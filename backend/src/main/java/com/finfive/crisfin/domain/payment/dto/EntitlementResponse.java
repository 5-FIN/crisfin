package com.finfive.crisfin.domain.payment.dto;

import com.finfive.crisfin.domain.payment.Entitlement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response of {@code GET /api/v1/payments/entitlement} and {@code POST /confirm}.
 *
 * @param active    whether the user currently has access
 * @param plan      plan name, or {@code null} when no entitlement exists
 * @param expiresAt expiry instant, or {@code null} when it does not expire
 */
@Getter
@Builder
public class EntitlementResponse {

    private final boolean active;
    private final String plan;
    private final LocalDateTime expiresAt;

    /** Maps an entitlement entity (nullable) into the response DTO. */
    public static EntitlementResponse from(Entitlement entitlement) {
        if (entitlement == null) {
            return EntitlementResponse.builder().active(false).build();
        }
        return EntitlementResponse.builder()
                .active(entitlement.isActive())
                .plan(entitlement.getPlan())
                .expiresAt(entitlement.getExpiresAt())
                .build();
    }
}

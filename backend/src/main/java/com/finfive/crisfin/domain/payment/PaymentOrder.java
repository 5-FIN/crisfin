package com.finfive.crisfin.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single payment order created at checkout and settled at confirm.
 *
 * <p>This is a mock payment record: {@code checkout} creates a {@code PENDING} order and
 * {@code confirm} transitions it to {@code PAID}, which in turn activates the user's
 * {@link Entitlement}. No real PG integration is performed.</p>
 */
@Entity
@Table(name = "payment_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uid", nullable = false, unique = true, length = 64)
    private String orderUid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Marks the order as paid. Idempotent-safe: callers should check status first. */
    public void markPaid() {
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public enum OrderStatus {
        PENDING, PAID, CANCELLED
    }
}

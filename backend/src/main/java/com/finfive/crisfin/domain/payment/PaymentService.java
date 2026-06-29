package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.domain.payment.dto.CheckoutResponse;
import com.finfive.crisfin.domain.payment.dto.EntitlementResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mock payment + entitlement service.
 *
 * <p>Checkout creates a {@code PENDING} order; confirm settles it and immediately activates
 * the user's {@link Entitlement} ("결제 후 즉시 해제"). No real payment gateway is involved —
 * the amount/plan are configured and the flow is deterministic so the front-end paywall can
 * be exercised end-to-end.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final EntitlementRepository entitlementRepository;

    @Value("${payment.price:9900}")
    private int price;

    @Value("${payment.plan:STANDARD}")
    private String plan;

    /** Whether the user currently holds an active entitlement. */
    @Transactional(readOnly = true)
    public boolean hasActiveEntitlement(Long userId) {
        return entitlementRepository.findByUserId(userId)
                .map(Entitlement::isActive)
                .orElse(false);
    }

    /** Returns the user's entitlement status (inactive when none exists). */
    @Transactional(readOnly = true)
    public EntitlementResponse getEntitlement(Long userId) {
        return EntitlementResponse.from(entitlementRepository.findByUserId(userId).orElse(null));
    }

    /** Creates a pending payment order and returns its uid + amount. */
    @Transactional
    public CheckoutResponse checkout(Long userId) {
        String orderUid = "order_" + UUID.randomUUID().toString().replace("-", "");
        PaymentOrder order = PaymentOrder.builder()
                .orderUid(orderUid)
                .userId(userId)
                .amount(price)
                .status(PaymentOrder.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        paymentOrderRepository.save(order);
        log.info("[PaymentService] checkout created order={} for userId={}", orderUid, userId);
        return CheckoutResponse.builder().orderUid(orderUid).amount(price).build();
    }

    /**
     * Confirms a pending order and activates the user's entitlement immediately.
     *
     * @return the now-active entitlement
     */
    @Transactional
    public EntitlementResponse confirm(Long userId, String orderUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new CrisfinException(ErrorCode.PAYMENT_ORDER_NOT_FOUND,
                        "결제 주문을 찾을 수 없습니다. orderUid=" + orderUid));

        if (!order.getUserId().equals(userId)) {
            throw new CrisfinException(ErrorCode.FORBIDDEN, "본인의 결제 주문이 아닙니다.");
        }

        if (!order.isPaid()) {
            order.markPaid();
        }

        Entitlement entitlement = entitlementRepository.findByUserId(userId).orElse(null);
        if (entitlement == null) {
            entitlement = Entitlement.builder()
                    .userId(userId)
                    .plan(plan)
                    .status(Entitlement.EntitlementStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .expiresAt(null)
                    .build();
            entitlementRepository.save(entitlement);
        } else {
            entitlement.activate(plan, null);
        }

        log.info("[PaymentService] order={} confirmed — entitlement active for userId={}", orderUid, userId);
        return EntitlementResponse.from(entitlement);
    }
}

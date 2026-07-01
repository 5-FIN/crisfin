package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.domain.payment.dto.CheckoutResponse;
import com.finfive.crisfin.domain.payment.dto.EntitlementResponse;
import com.finfive.crisfin.domain.payment.dto.PlanResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mock payment + entitlement service.
 *
 * <p>Checkout creates a {@code PENDING} order; confirm settles it and immediately activates
 * the user's {@link Entitlement} ("결제 후 즉시 해제"). No real payment gateway is involved —
 * 요금제는 {@link PaymentPlan} enum으로 정의되며 가격/유효기간/이용 횟수가 결정된다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final EntitlementRepository entitlementRepository;

    /** 구매 가능한 요금제 카탈로그. 무료(FREE)는 이용권 구매 대상이 아니므로 제외한다. */
    @Transactional(readOnly = true)
    public List<PlanResponse> plans() {
        return List.of(
                PlanResponse.from(PaymentPlan.SINGLE),
                PlanResponse.from(PaymentPlan.UNLIMITED_30D));
    }

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

    /** Creates a pending payment order for the requested plan and returns its uid + amount + plan. */
    @Transactional
    public CheckoutResponse checkout(Long userId, String planCode) {
        PaymentPlan p = PaymentPlan.fromCode(planCode);
        String orderUid = "order_" + UUID.randomUUID().toString().replace("-", "");
        PaymentOrder order = PaymentOrder.builder()
                .orderUid(orderUid)
                .userId(userId)
                .amount(p.getPriceKrw())
                .status(PaymentOrder.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .plan(p.name())
                .build();
        paymentOrderRepository.save(order);
        log.info("[PaymentService] checkout created order={} plan={} for userId={}", orderUid, p.name(), userId);
        return CheckoutResponse.builder()
                .orderUid(orderUid)
                .amount(p.getPriceKrw())
                .plan(p.name())
                .build();
    }

    /**
     * Confirms a pending order and activates the user's entitlement immediately.
     *
     * <p>주문에 기록된 요금제로 유효기간({@code now + durationDays})과 이용 횟수({@code uses},
     * {@code null}이면 무제한)를 산출해 이용권을 활성화한다.</p>
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

        PaymentPlan p = PaymentPlan.fromCode(order.getPlan());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(p.getDurationDays());
        Integer uses = p.getUses();

        Entitlement entitlement = entitlementRepository.findByUserId(userId).orElse(null);
        if (entitlement == null) {
            entitlement = Entitlement.builder()
                    .userId(userId)
                    .plan(p.name())
                    .status(Entitlement.EntitlementStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .expiresAt(expiresAt)
                    .remainingUses(uses)
                    .build();
            entitlementRepository.save(entitlement);
        } else {
            entitlement.activate(p.name(), expiresAt, uses);
        }

        log.info("[PaymentService] order={} confirmed — entitlement active plan={} for userId={}",
                orderUid, p.name(), userId);
        return EntitlementResponse.from(entitlement);
    }

    /**
     * 유료 분석 성공 시 이용권을 1회 소모한다.
     *
     * <p>이용권이 없으면 no-op. 무제한 요금제는 {@code remainingUses == null}이라 영향이 없고,
     * 유한 요금제만 남은 횟수를 1 차감한다.</p>
     */
    @Transactional
    public void consumeUse(Long userId) {
        Entitlement entitlement = entitlementRepository.findByUserId(userId).orElse(null);
        if (entitlement != null && entitlement.isActive()) {
            entitlement.consumeOneUse();
        }
    }
}

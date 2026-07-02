package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.Getter;

import java.util.List;

/**
 * 구매 가능한 요금제 카탈로그.
 *
 * <p>무료(FREE)는 별도 이용권(Entitlement)이 필요 없는 기본 상태이므로 여기에 포함하지 않는다.
 * 각 요금제는 가격, 유효기간(일), 이용 횟수({@code uses} — {@code null}은 무제한),
 * 마케팅 문구와 기능 목록을 함께 보유하여 프론트 요금제 페이지가 이 정보만으로 렌더링될 수 있게 한다.</p>
 */
@Getter
public enum PaymentPlan {

    SINGLE("1회 분석권", 4900, 30, 1, "가장 가볍게 시작",
            List.of("AI 맞춤 위기 분석 1회", "받을 돈·할 일·미룰 것 정리", "분석 결과 저장")),

    UNLIMITED_30D("30일 무제한", 29900, 30, null, "위기 상황 내내 든든하게",
            List.of("30일간 무제한 분석", "개인화 재분석 무제한", "모든 기능 이용"));

    /** 사용자에게 노출되는 요금제 이름. */
    private final String displayName;
    /** 가격 (원). */
    private final int priceKrw;
    /** 이용권 유효기간 (일). */
    private final int durationDays;
    /** 제공 이용 횟수. {@code null}이면 무제한. */
    private final Integer uses;
    /** 짧은 마케팅 문구. */
    private final String tagline;
    /** 기능 목록. */
    private final List<String> features;

    PaymentPlan(String displayName, int priceKrw, int durationDays, Integer uses,
                String tagline, List<String> features) {
        this.displayName = displayName;
        this.priceKrw = priceKrw;
        this.durationDays = durationDays;
        this.uses = uses;
        this.tagline = tagline;
        this.features = features;
    }

    /**
     * 요금제 코드({@link #name()})로 요금제를 조회한다.
     *
     * @throws CrisfinException {@link ErrorCode#PLAN_NOT_FOUND} 알 수 없는 코드일 때
     */
    public static PaymentPlan fromCode(String code) {
        try {
            return valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CrisfinException(ErrorCode.PLAN_NOT_FOUND, "알 수 없는 요금제: " + code);
        }
    }
}

package com.finfive.crisfin.domain.welfare.dto;

import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WelfareBenefitResponse} — 목록(from)은 상세 본문을 제외하고,
 * 상세(fromDetail)만 detailContent를 포함하는지 검증(목록 응답 비대화 방지).
 */
class WelfareBenefitResponseTest {

    private WelfareBenefit benefit() {
        return WelfareBenefit.builder()
                .id(1L)
                .serviceName("긴급복지 의료지원")
                .summary("요약")
                .detailContent("[개요] 상세 본문")
                .isActive(true)
                .build();
    }

    @Test
    void from_omitsDetailContent() {
        WelfareBenefitResponse res = WelfareBenefitResponse.from(benefit());

        assertThat(res.getServiceName()).isEqualTo("긴급복지 의료지원");
        assertThat(res.getDetailContent()).isNull(); // 목록엔 상세 본문 미포함
    }

    @Test
    void fromDetail_includesDetailContent() {
        WelfareBenefitResponse res = WelfareBenefitResponse.fromDetail(benefit());

        assertThat(res.getServiceName()).isEqualTo("긴급복지 의료지원");
        assertThat(res.getDetailContent()).isEqualTo("[개요] 상세 본문");
    }
}

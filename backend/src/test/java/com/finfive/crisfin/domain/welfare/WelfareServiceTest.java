package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WelfareService} — 위기태그 필터 라우팅 + 단건 조회.
 */
@ExtendWith(MockitoExtension.class)
class WelfareServiceTest {

    @Mock
    private WelfareBenefitRepository welfareBenefitRepository;

    @InjectMocks
    private WelfareService welfareService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getWelfareBenefits_withCrisisType_filtersByUppercasedTag() {
        when(welfareBenefitRepository
                .search(eq(null), eq(null), eq("UNEMPLOYMENT"), any()))
                .thenReturn(Page.<WelfareBenefit>empty());

        // 공백 + 소문자 입력 → trim + 대문자로 정규화돼야 함
        Page<WelfareBenefitResponse> result =
                welfareService.getWelfareBenefits(null, null, "  unemployment  ", pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(welfareBenefitRepository).search(null, null, "UNEMPLOYMENT", pageable);
    }

    @Test
    void getWelfareBenefits_blankFilters_passesAllNulls() {
        when(welfareBenefitRepository.search(eq(null), eq(null), eq(null), any()))
                .thenReturn(Page.<WelfareBenefit>empty());

        welfareService.getWelfareBenefits("", "  ", "", pageable);

        verify(welfareBenefitRepository).search(null, null, null, pageable);
    }

    @Test
    void getWelfareBenefits_withRegion_trimsAndPassesThrough() {
        when(welfareBenefitRepository
                .search(eq("서울특별시"), eq("종로구"), eq(null), any()))
                .thenReturn(Page.<WelfareBenefit>empty());

        welfareService.getWelfareBenefits("  서울특별시 ", " 종로구 ", null, pageable);

        verify(welfareBenefitRepository).search("서울특별시", "종로구", null, pageable);
    }

    @Test
    void getWelfareBenefit_found_mapsToResponse() {
        WelfareBenefit benefit = WelfareBenefit.builder()
                .id(1L)
                .serviceName("실업급여")
                .summary("고용보험 가입자 실업급여")
                .crisisTags(List.of("UNEMPLOYMENT"))
                .isActive(true)
                .build();
        when(welfareBenefitRepository.findById(1L)).thenReturn(Optional.of(benefit));

        WelfareBenefitResponse res = welfareService.getWelfareBenefit(1L);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getServiceName()).isEqualTo("실업급여");
        assertThat(res.getCrisisTags()).containsExactly("UNEMPLOYMENT");
    }

    @Test
    void getWelfareBenefit_notFound_throwsWelfareApiUnavailable() {
        when(welfareBenefitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> welfareService.getWelfareBenefit(99L))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.WELFARE_API_UNAVAILABLE);
    }

    /** PageImpl 매핑이 실제로 동작하는지 확인(비어있지 않은 페이지). */
    @Test
    void getWelfareBenefits_mapsNonEmptyPage() {
        WelfareBenefit benefit = WelfareBenefit.builder()
                .id(2L).serviceName("긴급복지").crisisTags(List.of("HOSPITALIZATION")).build();
        when(welfareBenefitRepository.search(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(benefit), pageable, 1));

        Page<WelfareBenefitResponse> result =
                welfareService.getWelfareBenefits(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getServiceName()).isEqualTo("긴급복지");
    }
}

package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WelfareFavoriteService} — 404/멱등/목록 정렬·누락 스킵.
 */
@ExtendWith(MockitoExtension.class)
class WelfareFavoriteServiceTest {

    @Mock
    private WelfareFavoriteRepository welfareFavoriteRepository;
    @Mock
    private WelfareBenefitRepository welfareBenefitRepository;

    @InjectMocks
    private WelfareFavoriteService service;

    @Test
    void addFavorite_nonexistentBenefit_throwsWelfareNotFound() {
        when(welfareBenefitRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.addFavorite(1L, 99L))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.WELFARE_NOT_FOUND);

        verify(welfareFavoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_new_saves() {
        when(welfareBenefitRepository.existsById(1L)).thenReturn(true);
        when(welfareFavoriteRepository.existsByUserIdAndWelfareBenefitId(7L, 1L)).thenReturn(false);

        service.addFavorite(7L, 1L);

        verify(welfareFavoriteRepository).save(any(WelfareFavorite.class));
    }

    @Test
    void addFavorite_alreadyFavorited_isNoOp() {
        when(welfareBenefitRepository.existsById(1L)).thenReturn(true);
        when(welfareFavoriteRepository.existsByUserIdAndWelfareBenefitId(7L, 1L)).thenReturn(true);

        service.addFavorite(7L, 1L);

        verify(welfareFavoriteRepository, never()).save(any());
    }

    @Test
    void removeFavorite_delegatesToDelete() {
        service.removeFavorite(7L, 1L);
        verify(welfareFavoriteRepository).deleteByUserIdAndWelfareBenefitId(7L, 1L);
    }

    @Test
    void listFavorites_keepsFavoriteOrder_andSkipsMissingBenefits() {
        // 즐겨찾기 순서: benefitId 2, 1, 3 (3은 더 이상 존재하지 않음)
        when(welfareFavoriteRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                WelfareFavorite.of(7L, 2L),
                WelfareFavorite.of(7L, 1L),
                WelfareFavorite.of(7L, 3L)));
        // findAllById는 순서를 보장하지 않는다(1,2만 반환 — 3은 삭제됨)
        when(welfareBenefitRepository.findAllById(List.of(2L, 1L, 3L))).thenReturn(List.of(
                WelfareBenefit.builder().id(1L).serviceName("하나").isActive(true).build(),
                WelfareBenefit.builder().id(2L).serviceName("둘").isActive(true).build()));

        List<WelfareBenefitResponse> result = service.listFavorites(7L);

        // 즐겨찾기 순서(2, 1)로 정렬되고, 없는 3은 빠져야 함
        assertThat(result).extracting(WelfareBenefitResponse::getId).containsExactly(2L, 1L);
    }
}

package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalysisService}의 공유 링크 로직.
 *
 * <p>공유 메서드(createShareLink/revokeShareLink/getSharedResult의 404 경로)는
 * {@code analysisResultRepository}만 사용하므로 나머지 협력자는 Mockito가 null로 주입한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceShareTest {

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private AnalysisResult result(Long userId, String shareToken) {
        return AnalysisResult.builder()
                .id(5L)
                .userId(userId)
                .crisisType(CrisisType.HOSPITALIZATION)
                .situationDescription("t")
                .shareToken(shareToken)
                .build();
    }

    @Test
    void createShareLink_notOwner_throwsAnalysisNotFound() {
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.of(result(2L, null)));

        assertThatThrownBy(() -> analysisService.createShareLink(5L, 1L))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_NOT_FOUND);
    }

    @Test
    void createShareLink_missingId_throwsAnalysisNotFound() {
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.createShareLink(5L, 1L))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_NOT_FOUND);
    }

    @Test
    void createShareLink_ownerWithoutToken_generatesUnguessableToken() {
        AnalysisResult r = result(1L, null);
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.of(r));

        String token = analysisService.createShareLink(5L, 1L);

        assertThat(token).isNotNull().hasSize(32); // UUID(하이픈 제거) = 32자
        assertThat(r.getShareToken()).isEqualTo(token); // 엔티티에 반영
    }

    @Test
    void createShareLink_ownerWithToken_isIdempotent() {
        AnalysisResult r = result(1L, "existing_token");
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.of(r));

        String token = analysisService.createShareLink(5L, 1L);

        assertThat(token).isEqualTo("existing_token"); // 기존 토큰 재사용
    }

    @Test
    void revokeShareLink_owner_clearsToken() {
        AnalysisResult r = result(1L, "some_token");
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.of(r));

        analysisService.revokeShareLink(5L, 1L);

        assertThat(r.getShareToken()).isNull();
    }

    @Test
    void revokeShareLink_notOwner_throwsAnalysisNotFound() {
        when(analysisResultRepository.findById(5L)).thenReturn(Optional.of(result(2L, "t")));

        assertThatThrownBy(() -> analysisService.revokeShareLink(5L, 1L))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_NOT_FOUND);
    }

    @Test
    void getSharedResult_unknownToken_throwsAnalysisNotFound() {
        when(analysisResultRepository.findByShareToken("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.getSharedResult("bogus"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_NOT_FOUND);
    }
}

package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.analysis.dto.AnalysisRequest;
import com.finfive.crisfin.domain.analysis.dto.AnalysisResultResponse;
import com.finfive.crisfin.domain.analysis.dto.ReinferRequest;
import com.finfive.crisfin.domain.analysis.dto.ShareLinkResponse;
import com.finfive.crisfin.domain.payment.PaymentService;
import com.finfive.crisfin.domain.user.User;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalysisController} — 결제 게이팅(401/402/200)을 컨트롤러 직접 호출로 검증.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private AnalysisController controller;

    private final AnalysisRequest request = mock(AnalysisRequest.class);
    private final AnalysisResultResponse result = mock(AnalysisResultResponse.class);

    private User user(long id) {
        return User.builder()
                .id(id)
                .email("u@test.com")
                .passwordHash("hash")
                .nickname("tester")
                .build();
    }

    @Test
    void recommend_anonymous_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.recommend(request, null))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void recommend_authenticatedWithoutEntitlement_throwsPaymentRequired() {
        when(paymentService.hasActiveEntitlement(1L)).thenReturn(false);

        assertThatThrownBy(() -> controller.recommend(request, user(1L)))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_REQUIRED);
    }

    @Test
    void recommend_paidUser_returnsOkWithResult() {
        when(paymentService.hasActiveEntitlement(1L)).thenReturn(true);
        when(analysisService.recommend(request, 1L)).thenReturn(result);

        ResponseEntity<ApiResponse<AnalysisResultResponse>> res =
                controller.recommend(request, user(1L));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData()).isSameAs(result);
    }

    @Test
    void reinfer_anonymous_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.reinfer(5L, null, null))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void reinfer_paidUser_nullBody_usesEmptyRequest() {
        when(paymentService.hasActiveEntitlement(1L)).thenReturn(true);
        when(analysisService.reinfer(eq(5L), any(ReinferRequest.class), eq(1L))).thenReturn(result);

        ResponseEntity<ApiResponse<AnalysisResultResponse>> res =
                controller.reinfer(5L, null, user(1L));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getData()).isSameAs(result);
    }

    @Test
    void getResult_authenticated_delegatesToServiceWithUserId() {
        when(analysisService.getResult(7L, 1L)).thenReturn(result);

        ResponseEntity<ApiResponse<AnalysisResultResponse>> res = controller.getResult(7L, user(1L));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getData()).isSameAs(result);
    }

    @Test
    void getResult_anonymous_throwsUnauthorized() {
        // 로그인 없이 결과 조회 시도 → 401 (IDOR 방지: 익명 접근 차단)
        assertThatThrownBy(() -> controller.getResult(7L, null))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void share_anonymous_throwsUnauthorized() {
        // 로그인 없이 공유 링크 생성 시도 → 401
        assertThatThrownBy(() -> controller.share(5L, null))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void share_authenticated_returnsToken() {
        when(analysisService.createShareLink(5L, 1L)).thenReturn("tok123");

        ResponseEntity<ApiResponse<ShareLinkResponse>> res = controller.share(5L, user(1L));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getData().getShareToken()).isEqualTo("tok123");
    }

    @Test
    void getShared_public_returnsResult() {
        // 토큰만으로 공개 조회(인증 없음)
        when(analysisService.getSharedResult("tok123")).thenReturn(result);

        ResponseEntity<ApiResponse<AnalysisResultResponse>> res = controller.getShared("tok123");

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getData()).isSameAs(result);
    }
}

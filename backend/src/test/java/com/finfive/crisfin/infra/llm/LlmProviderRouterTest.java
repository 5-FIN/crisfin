package com.finfive.crisfin.infra.llm;

import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LlmProviderRouter} — 우선순위 기반 폴백 체인.
 */
class LlmProviderRouterTest {

    private final LlmRequest request = mock(LlmRequest.class);
    private final LlmResponse response = mock(LlmResponse.class);

    private LlmProvider provider(String name) {
        LlmProvider p = mock(LlmProvider.class);
        when(p.getProviderName()).thenReturn(name);
        return p;
    }

    private LlmProviderRouter router(List<LlmProvider> providers, String order) {
        LlmProviderRouter router = new LlmProviderRouter(providers);
        ReflectionTestUtils.setField(router, "providerOrderStr", order);
        return router;
    }

    @Test
    void complete_firstProviderSucceeds_doesNotCallOthers() {
        LlmProvider gemini = provider("GEMINI");
        LlmProvider claude = provider("CLAUDE");
        when(gemini.complete(request)).thenReturn(response);

        LlmResponse result = router(List.of(gemini, claude), "GEMINI,CLAUDE").complete(request);

        assertThat(result).isSameAs(response);
        verify(claude, never()).complete(any());
    }

    @Test
    void complete_firstProviderFails_fallsBackToNext() {
        LlmProvider gemini = provider("GEMINI");
        LlmProvider claude = provider("CLAUDE");
        when(gemini.complete(request)).thenThrow(new RuntimeException("provider down"));
        when(claude.complete(request)).thenReturn(response);

        LlmResponse result = router(List.of(gemini, claude), "GEMINI,CLAUDE").complete(request);

        assertThat(result).isSameAs(response);
        verify(gemini).complete(request);
        verify(claude).complete(request);
    }

    @Test
    void complete_allProvidersFail_throwsAllProvidersFailed() {
        LlmProvider gemini = provider("GEMINI");
        LlmProvider claude = provider("CLAUDE");
        when(gemini.complete(request)).thenThrow(new RuntimeException("down"));
        when(claude.complete(request)).thenThrow(new RuntimeException("down"));

        LlmProviderRouter router = router(List.of(gemini, claude), "GEMINI,CLAUDE");

        assertThatThrownBy(() -> router.complete(request))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_ALL_PROVIDERS_FAILED);
    }

    @Test
    void complete_unknownNameInOrder_isSkipped() {
        LlmProvider gemini = provider("GEMINI");
        when(gemini.complete(request)).thenReturn(response);

        // 'FOO'는 등록되지 않은 이름 → 건너뛰고 GEMINI로 진행
        LlmResponse result = router(List.of(gemini), "FOO,GEMINI").complete(request);

        assertThat(result).isSameAs(response);
    }
}

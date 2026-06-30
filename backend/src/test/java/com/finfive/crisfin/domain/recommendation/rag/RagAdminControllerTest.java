package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.recommendation.rag.dto.ReindexResponse;
import com.finfive.crisfin.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RagAdminController} — reindex 위임 + 응답 매핑.
 * (보안 @PreAuthorize는 Spring 런타임에서 강제됨; 여기선 컨트롤러 로직만 검증)
 */
@ExtendWith(MockitoExtension.class)
class RagAdminControllerTest {

    @Mock
    private PolicyIndexingService policyIndexingService;

    @InjectMocks
    private RagAdminController controller;

    @Test
    void reindex_returnsIndexedCount() {
        when(policyIndexingService.reindexAll()).thenReturn(15);

        ResponseEntity<ApiResponse<ReindexResponse>> res = controller.reindex();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData().indexedChunks()).isEqualTo(15);
        verify(policyIndexingService).reindexAll();
    }

    @Test
    void reindex_disabledOrNoData_returnsZero() {
        when(policyIndexingService.reindexAll()).thenReturn(0);

        ResponseEntity<ApiResponse<ReindexResponse>> res = controller.reindex();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getData().indexedChunks()).isZero();
    }
}

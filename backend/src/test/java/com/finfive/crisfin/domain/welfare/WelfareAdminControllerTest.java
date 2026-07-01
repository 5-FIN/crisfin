package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.global.response.ApiResponse;
import com.finfive.crisfin.global.scheduler.WelfareSyncScheduler;
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
 * Unit tests for {@link WelfareAdminController} — sync 위임 + 응답 매핑.
 * (보안 @PreAuthorize는 Spring 런타임에서 강제됨; 여기선 컨트롤러 로직만 검증)
 */
@ExtendWith(MockitoExtension.class)
class WelfareAdminControllerTest {

    @Mock
    private WelfareSyncScheduler welfareSyncScheduler;

    @InjectMocks
    private WelfareAdminController controller;

    @Test
    void sync_returnsProcessedCount() {
        when(welfareSyncScheduler.runSync()).thenReturn(42);

        ResponseEntity<ApiResponse<Integer>> res = controller.sync();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData()).isEqualTo(42);
        verify(welfareSyncScheduler).runSync();
    }

    @Test
    void sync_notConfiguredOrNoData_returnsZero() {
        when(welfareSyncScheduler.runSync()).thenReturn(0);

        ResponseEntity<ApiResponse<Integer>> res = controller.sync();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getData()).isZero();
    }
}

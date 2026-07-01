package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.global.response.ApiResponse;
import com.finfive.crisfin.global.scheduler.WelfareSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin operations for welfare-benefit synchronisation.
 *
 * <p>Manually triggers a pull from the LocalGovernment welfare API and upserts the
 * results into the local {@code welfare_benefits} table. It is a privileged operation
 * (external API call + DB writes + RAG reindex), so it is restricted to {@code ADMIN} users.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/welfare")
@RequiredArgsConstructor
public class WelfareAdminController {

    private final WelfareSyncScheduler welfareSyncScheduler;

    /**
     * Runs the welfare sync on demand.
     *
     * <p>Returns the number of welfare records processed. {@code 0} means the API key is
     * not configured or the external API returned no data — the call still succeeds.</p>
     */
    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> sync() {
        int processed = welfareSyncScheduler.runSync();
        return ResponseEntity.ok(ApiResponse.ok(processed));
    }
}

package com.finfive.crisfin.global.scheduler;

import com.finfive.crisfin.domain.welfare.WelfareBenefitRepository;
import com.finfive.crisfin.infra.openapi.WelfareApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Populates the {@code welfare_benefits} table on application startup so a freshly
 * provisioned database (a new deploy, or {@code docker compose up} with an empty volume)
 * serves region/crisis welfare data without waiting for the 3 AM cron or a manual admin call.
 *
 * <p>Runs at most once per empty database. It is a no-op — with no external API cost — when:
 * <ul>
 *   <li>the {@code welfare.sync.on-startup} flag is disabled, or</li>
 *   <li>the welfare API key is not configured ({@code PUBLIC_DATA_API_KEY} blank), or</li>
 *   <li>the table already contains rows (so restarts never re-sync).</li>
 * </ul>
 *
 * <p>The sync runs on a background daemon thread and is wrapped in try/catch: the multi-minute
 * paginated fetch + detail enrichment must never delay server readiness nor crash startup. The
 * daily {@link WelfareSyncScheduler} and {@code POST /api/v1/admin/welfare/sync} remain the
 * paths for refreshing an already-populated table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareSyncBootstrap implements ApplicationRunner {

    private final WelfareApiClient welfareApiClient;
    private final WelfareBenefitRepository welfareBenefitRepository;
    private final WelfareSyncScheduler welfareSyncScheduler;

    @Value("${welfare.sync.on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            log.info("[WelfareSyncBootstrap] sync-on-startup disabled — skipping.");
            return;
        }
        if (!welfareApiClient.isConfigured()) {
            log.info("[WelfareSyncBootstrap] welfare API key not configured — skipping startup sync.");
            return;
        }

        long existing = welfareBenefitRepository.count();
        if (existing > 0) {
            log.info("[WelfareSyncBootstrap] welfare table already populated ({} row(s)) — skipping startup sync.",
                    existing);
            return;
        }

        // Empty table: run the (slow) paginated sync off the boot thread so readiness is not delayed.
        log.info("[WelfareSyncBootstrap] welfare table empty — running startup sync in the background ...");
        Thread worker = new Thread(this::syncQuietly, "welfare-sync-bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private void syncQuietly() {
        try {
            int processed = welfareSyncScheduler.runSync();
            log.info("[WelfareSyncBootstrap] startup sync complete — {} record(s).", processed);
        } catch (Exception ex) {
            log.error("[WelfareSyncBootstrap] startup sync failed (server unaffected): {}",
                    ex.getMessage(), ex);
        }
    }
}

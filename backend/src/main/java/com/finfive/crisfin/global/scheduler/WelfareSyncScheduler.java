package com.finfive.crisfin.global.scheduler;

import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import com.finfive.crisfin.domain.welfare.WelfareBenefitRepository;
import com.finfive.crisfin.infra.openapi.WelfareApiClient;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse.WelfareItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scheduled job that pulls welfare-benefit data from the external Government
 * Welfare API and upserts it into the local {@code welfare_benefits} table.
 *
 * <p>Runs daily at 03:00 (server local time). If the API key is not configured
 * the job logs and skips rather than raising an error. All exceptions are caught
 * and logged so that a transient API failure cannot crash the scheduler thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareSyncScheduler {

    private final WelfareApiClient welfareApiClient;
    private final WelfareBenefitRepository welfareBenefitRepository;

    /**
     * Synchronises welfare benefits from the external API.
     *
     * <p>Execution policy:
     * <ul>
     *   <li>Skip entirely when the API key is blank (dev / CI environments).</li>
     *   <li>Upsert by {@code externalServiceId}: update existing rows, insert new ones.</li>
     *   <li>Catch all exceptions and log — never re-throw to prevent scheduler thread death.</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncWelfareBenefits() {
        // Guard: skip when no API key is configured (local / CI environments)
        if (!welfareApiClient.isConfigured()) {
            log.info("[WelfareSyncScheduler] API key is not configured — skipping welfare sync.");
            return;
        }

        log.info("[WelfareSyncScheduler] Starting welfare benefits sync ...");

        try {
            WelfareApiResponse response = welfareApiClient.getWelfareBenefits(1, 100);
            List<WelfareItem> items = response.getData();

            if (items == null || items.isEmpty()) {
                log.warn("[WelfareSyncScheduler] External API returned 0 items — nothing to sync.");
                return;
            }

            LocalDateTime syncedAt = LocalDateTime.now();
            List<WelfareBenefit> toSave = new ArrayList<>(items.size());

            for (WelfareItem item : items) {
                WelfareBenefit entity = welfareBenefitRepository
                        .findByExternalServiceId(item.getSvcId())
                        .orElse(null);

                if (entity != null) {
                    // Update existing record — JPA dirty-checking will flush on commit
                    entity.update(
                            item.getSrvNm(),
                            item.getSvcSumry(),
                            item.getTgtrDtlCn(),
                            item.getSlctCrtDvCd(),
                            item.getAplyMtdCn(),
                            item.getSvcUrl(),
                            item.getJrsdInstNm(),
                            /* contact */ null,
                            /* crisisTags — enriched in a later step */ Collections.emptyList(),
                            syncedAt
                    );
                    toSave.add(entity);
                } else {
                    toSave.add(WelfareBenefit.builder()
                            .externalServiceId(item.getSvcId())
                            .serviceName(item.getSrvNm())
                            .summary(item.getSvcSumry())
                            .targetDescription(item.getTgtrDtlCn())
                            .selectionCriteria(item.getSlctCrtDvCd())
                            .applyMethod(item.getAplyMtdCn())
                            .applyUrl(item.getSvcUrl())
                            .ministryName(item.getJrsdInstNm())
                            .contact(null)
                            .crisisTags(Collections.emptyList())
                            .isActive(true)
                            .lastSyncedAt(syncedAt)
                            .build());
                }
            }

            welfareBenefitRepository.saveAll(toSave);

            log.info("[WelfareSyncScheduler] Welfare sync complete — {} record(s) processed.", toSave.size());

        } catch (Exception ex) {
            // Log but do NOT re-throw: prevents the @Scheduled executor from suppressing
            // future executions due to an uncaught exception on the scheduler thread.
            log.error("[WelfareSyncScheduler] Welfare sync failed: {}", ex.getMessage(), ex);
        }
    }
}

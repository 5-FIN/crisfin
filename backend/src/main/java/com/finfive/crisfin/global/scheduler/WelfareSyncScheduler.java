package com.finfive.crisfin.global.scheduler;

import com.finfive.crisfin.domain.recommendation.rag.PolicyIndexingService;
import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import com.finfive.crisfin.domain.welfare.WelfareBenefitRepository;
import com.finfive.crisfin.domain.welfare.WelfareCrisisTagger;
import com.finfive.crisfin.infra.openapi.WelfareApiClient;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse.WelfareItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled job that pulls welfare-benefit data from the LocalGovernment welfare
 * API and upserts it into the local {@code welfare_benefits} table.
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
    private final PolicyIndexingService policyIndexingService;
    private final WelfareCrisisTagger crisisTagger;

    /**
     * Scheduled entry point. Delegates to {@link #runSync()} so the same logic can
     * be triggered manually (e.g. via the admin endpoint).
     *
     * <p>All exceptions are caught and logged — never re-thrown — to prevent the
     * {@code @Scheduled} executor from suppressing future executions.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncWelfareBenefits() {
        try {
            runSync();
        } catch (Exception ex) {
            log.error("[WelfareSyncScheduler] Welfare sync failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Synchronises welfare benefits from the external API and returns the number of
     * records processed.
     *
     * <p>Execution policy:
     * <ul>
     *   <li>Skip entirely when the API key is blank (dev / CI environments) — returns 0.</li>
     *   <li>Upsert by {@code externalServiceId}: update existing rows, insert new ones.</li>
     *   <li>Rebuild the RAG index afterwards, isolated so an index failure cannot break the sync.</li>
     * </ul>
     *
     * @return number of welfare records upserted (0 when skipped or no data)
     */
    public int runSync() {
        // Guard: skip when no API key is configured (local / CI environments)
        if (!welfareApiClient.isConfigured()) {
            log.info("[WelfareSyncScheduler] API key is not configured — skipping welfare sync.");
            return 0;
        }

        log.info("[WelfareSyncScheduler] Starting welfare benefits sync ...");

        WelfareApiResponse response = welfareApiClient.getWelfareBenefits(1, 100);
        List<WelfareItem> items = response.getData();

        if (items == null || items.isEmpty()) {
            log.warn("[WelfareSyncScheduler] External API returned 0 items — nothing to sync.");
            return 0;
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        List<WelfareBenefit> toSave = new ArrayList<>(items.size());

        for (WelfareItem item : items) {
            String targetDescription = buildTargetDescription(item);
            // 위기유형 태그를 서비스명·요약·관심주제·생애주기 텍스트에서 키워드로 도출
            List<String> crisisTags = crisisTagger.tag(
                    item.getServNm(), item.getServDgst(),
                    item.getIntrsThemaNmArray(), item.getLifeNmArray());

            WelfareBenefit entity = welfareBenefitRepository
                    .findByExternalServiceId(item.getServId())
                    .orElse(null);

            if (entity != null) {
                // Update existing record — persisted explicitly via saveAll below
                entity.update(
                        item.getServNm(),
                        item.getServDgst(),
                        targetDescription,
                        item.getIntrsThemaNmArray(),
                        item.getAplyMtdNm(),
                        item.getServDtlLink(),
                        item.getBizChrDeptNm(),
                        /* contact */ null,
                        crisisTags,
                        item.getCtpvNm(),
                        item.getSggNm(),
                        syncedAt
                );
                toSave.add(entity);
            } else {
                toSave.add(WelfareBenefit.builder()
                        .externalServiceId(item.getServId())
                        .serviceName(item.getServNm())
                        .summary(item.getServDgst())
                        .targetDescription(targetDescription)
                        .selectionCriteria(item.getIntrsThemaNmArray())
                        .applyMethod(item.getAplyMtdNm())
                        .applyUrl(item.getServDtlLink())
                        .ministryName(item.getBizChrDeptNm())
                        .contact(null)
                        .crisisTags(crisisTags)
                        .ctpvNm(item.getCtpvNm())
                        .sggNm(item.getSggNm())
                        .isActive(true)
                        .lastSyncedAt(syncedAt)
                        .build());
            }
        }

        welfareBenefitRepository.saveAll(toSave);

        log.info("[WelfareSyncScheduler] Welfare sync complete — {} record(s) processed.", toSave.size());

        // Rebuild the RAG policy index from the freshly-synced data. Isolated in its own
        // try/catch so an embedding/index failure can never break the welfare sync.
        try {
            int indexed = policyIndexingService.reindexAll();
            log.info("[WelfareSyncScheduler] Policy reindex complete — {} chunk(s).", indexed);
        } catch (Exception ragEx) {
            log.error("[WelfareSyncScheduler] Policy reindex failed (welfare sync unaffected): {}",
                    ragEx.getMessage(), ragEx);
        }

        return toSave.size();
    }

    /**
     * Builds the {@code targetDescription} from region and life-cycle fields.
     * Format: {@code "[지역] {ctpvNm} {sggNm}"} optionally followed by
     * {@code " / [생애주기] {lifeNmArray}"} when the life-cycle field is present.
     */
    private String buildTargetDescription(WelfareItem item) {
        StringBuilder sb = new StringBuilder("[지역] ")
                .append(item.getCtpvNm() != null ? item.getCtpvNm() : "")
                .append(" ")
                .append(item.getSggNm() != null ? item.getSggNm() : "");
        if (StringUtils.hasText(item.getLifeNmArray())) {
            sb.append(" / [생애주기] ").append(item.getLifeNmArray());
        }
        return sb.toString();
    }
}

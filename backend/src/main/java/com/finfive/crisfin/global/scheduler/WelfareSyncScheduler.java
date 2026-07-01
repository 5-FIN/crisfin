package com.finfive.crisfin.global.scheduler;

import com.finfive.crisfin.domain.recommendation.rag.PolicyIndexingService;
import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import com.finfive.crisfin.domain.welfare.WelfareBenefitRepository;
import com.finfive.crisfin.domain.welfare.WelfareCrisisTagger;
import com.finfive.crisfin.infra.openapi.WelfareApiClient;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse.WelfareItem;
import com.finfive.crisfin.infra.openapi.dto.WelfareDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    /** 동기화할 최대 항목 수 (페이지네이션 상한). */
    @Value("${welfare.api.max-items:1000}")
    private int maxItems;

    /** 페이지당 요청 항목 수. */
    @Value("${welfare.api.page-size:100}")
    private int pageSize;

    /** 상세조회 병렬 처리 스레드 수 (외부 API TPS 한도를 넘지 않는 선). */
    @Value("${welfare.api.detail-concurrency:8}")
    private int detailConcurrency;

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

        // Paginate: fetch pageNo = 1, 2, ... (numOfRows = pageSize) accumulating items until
        //   - accumulated >= maxItems, OR
        //   - accumulated >= totalCount (from the first response), OR
        //   - a page returns empty.
        List<WelfareItem> items = new ArrayList<>();
        int totalCount = Integer.MAX_VALUE; // ceiling — overwritten by the first response
        for (int page = 1; items.size() < maxItems; page++) {
            WelfareApiResponse response = welfareApiClient.getWelfareBenefits(page, pageSize);
            if (page == 1) {
                totalCount = response.getTotalCount();
            }

            List<WelfareItem> pageItems = response.getData();
            if (pageItems == null || pageItems.isEmpty()) {
                break;
            }
            items.addAll(pageItems);

            if (totalCount > 0 && items.size() >= totalCount) {
                break;
            }
        }

        if (items.isEmpty()) {
            log.warn("[WelfareSyncScheduler] External API returned 0 items — nothing to sync.");
            return 0;
        }

        // Cap at maxItems (a page may have overshot the limit).
        if (items.size() > maxItems) {
            items = new ArrayList<>(items.subList(0, maxItems));
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

        // Detail-enrichment pass (parallelized): fetch each service's FULL detail text
        // concurrently on a bounded pool and store it as the rich RAG ingest source. Each
        // task modifies a distinct entity object, so there is no shared-state race; failures
        // are isolated per item and only skip that one. Bounded concurrency keeps us under
        // the external API's TPS limit.
        List<WelfareBenefit> withServId = new ArrayList<>();
        for (WelfareBenefit e : toSave) {
            if (StringUtils.hasText(e.getExternalServiceId())) {
                withServId.add(e);
            }
        }
        int total = withServId.size();

        int concurrency = Math.max(1, detailConcurrency);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        AtomicInteger done = new AtomicInteger();
        List<WelfareBenefit> detailed = Collections.synchronizedList(new ArrayList<>());
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(total);
            for (WelfareBenefit entity : withServId) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        WelfareDetailResponse detail =
                                welfareApiClient.getWelfareDetail(entity.getExternalServiceId());
                        if (detail != null) {
                            String detailContent = detail.toDetailContent();
                            if (StringUtils.hasText(detailContent)) {
                                entity.applyDetailContent(detailContent);
                                detailed.add(entity);
                            }
                        }
                    } catch (Exception detailEx) {
                        // 개별 상세 실패는 건너뛴다 (클라이언트가 이미 null을 반환하지만 방어적으로 감싼다).
                        log.debug("[WelfareSyncScheduler] detail fetch skipped (servId={}): {}",
                                entity.getExternalServiceId(), detailEx.getMessage());
                    } finally {
                        int n = done.incrementAndGet();
                        if (n % 100 == 0) {
                            log.info("[WelfareSyncScheduler] detail {}/{}", n, total);
                        }
                    }
                }, pool));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            pool.shutdown();
        }

        if (!detailed.isEmpty()) {
            welfareBenefitRepository.saveAll(detailed);
        }
        log.info("[WelfareSyncScheduler] Detail enrichment complete — {}/{} enriched.",
                detailed.size(), total);

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

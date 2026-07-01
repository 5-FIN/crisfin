package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.infra.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the {@code policy_embeddings} vector store on application startup so a freshly
 * provisioned database (e.g. a new branch, a new deploy, or {@code docker compose up} with
 * an empty volume) serves RAG queries without a manual admin reindex.
 *
 * <p>Runs at most once per empty database. It is a no-op — with no OpenAI cost — when:
 * <ul>
 *   <li>the {@code embedding.rag.auto-index-on-startup} flag is disabled, or</li>
 *   <li>embeddings are disabled ({@code OPENAI_API_KEY} blank), or</li>
 *   <li>the store already contains rows (so restarts never re-embed).</li>
 * </ul>
 *
 * <p>The actual embedding call runs on a background thread and is wrapped in try/catch: a
 * slow or failing OpenAI call must never delay server readiness nor crash startup. The
 * daily {@link com.finfive.crisfin.global.scheduler.WelfareSyncScheduler} and the admin
 * {@code POST /api/v1/admin/rag/reindex} endpoint remain the paths for refreshing an
 * already-populated index.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyIndexBootstrap implements ApplicationRunner {

    private final EmbeddingProvider embeddingProvider;
    private final PolicyEmbeddingRepository policyEmbeddingRepository;
    private final PolicyIndexingService policyIndexingService;

    @Value("${embedding.rag.auto-index-on-startup:true}")
    private boolean autoIndexOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoIndexOnStartup) {
            log.info("[PolicyIndexBootstrap] auto-index-on-startup disabled — skipping.");
            return;
        }
        if (!embeddingProvider.isEnabled()) {
            log.info("[PolicyIndexBootstrap] embeddings disabled (no OPENAI_API_KEY) — skipping startup index.");
            return;
        }

        long existing = policyEmbeddingRepository.count();
        if (existing > 0) {
            log.info("[PolicyIndexBootstrap] policy index already populated ({} row(s)) — skipping startup index.",
                    existing);
            return;
        }

        // Empty store: build the index off the boot thread so a slow/failing OpenAI call
        // never delays server readiness or crashes startup.
        log.info("[PolicyIndexBootstrap] policy index empty — building it in the background ...");
        Thread worker = new Thread(this::indexQuietly, "policy-index-bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private void indexQuietly() {
        try {
            int indexed = policyIndexingService.reindexAll();
            log.info("[PolicyIndexBootstrap] startup index complete — {} chunk(s).", indexed);
        } catch (Exception ex) {
            log.error("[PolicyIndexBootstrap] startup index failed (server unaffected): {}",
                    ex.getMessage(), ex);
        }
    }
}

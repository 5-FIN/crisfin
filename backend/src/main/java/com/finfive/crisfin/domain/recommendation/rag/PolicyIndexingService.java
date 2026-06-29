package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.guide.CrisisGuide;
import com.finfive.crisfin.domain.guide.CrisisGuideRepository;
import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import com.finfive.crisfin.domain.welfare.WelfareBenefitRepository;
import com.finfive.crisfin.infra.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Write side of the hybrid RAG pipeline: (re)builds the {@code policy_embeddings} vector
 * store from the local welfare-benefit and crisis-guide tables.
 *
 * <p>No-op (returns {@code 0}) when embeddings are disabled, so environments without an
 * {@code OPENAI_API_KEY} behave exactly as before. The store is rebuilt atomically:
 * embeddings are computed first, then the table is cleared and repopulated within a single
 * transaction, so a failure leaves the previous index intact.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PolicyIndexingService {

    private final EmbeddingProvider embeddingProvider;
    private final PolicyEmbeddingRepository policyEmbeddingRepository;
    private final WelfareBenefitRepository welfareBenefitRepository;
    private final CrisisGuideRepository crisisGuideRepository;

    /**
     * Rebuilds the entire policy index.
     *
     * @return number of chunks indexed (0 when disabled or no source data)
     */
    @Transactional
    public int reindexAll() {
        if (!embeddingProvider.isEnabled()) {
            log.info("[PolicyIndexingService] embeddings disabled — skipping reindex.");
            return 0;
        }

        List<Chunk> chunks = new ArrayList<>();

        for (WelfareBenefit w : welfareBenefitRepository.findAll()) {
            String content = joinNonBlank(
                    w.getServiceName(),
                    w.getSummary(),
                    w.getTargetDescription(),
                    w.getSelectionCriteria(),
                    w.getApplyMethod());
            if (content.isBlank()) {
                continue;
            }
            chunks.add(new Chunk("WELFARE", String.valueOf(w.getId()),
                    wrapTags(w.getCrisisTags()), content));
        }

        for (CrisisGuide g : crisisGuideRepository.findAll()) {
            String keyRules = (g.getKeyRules() == null) ? null : String.join(" ", g.getKeyRules());
            String content = joinNonBlank(g.getTitle(), g.getCoachingPrompt(), keyRules);
            if (content.isBlank()) {
                continue;
            }
            chunks.add(new Chunk("GUIDE", String.valueOf(g.getId()),
                    wrapTags(List.of(g.getCrisisType().name())), content));
        }

        if (chunks.isEmpty()) {
            policyEmbeddingRepository.deleteAll();
            log.info("[PolicyIndexingService] no source policies to index — store cleared.");
            return 0;
        }

        // Embed first; only clear + repopulate once vectors are in hand.
        List<float[]> vectors = embeddingProvider.embedAll(
                chunks.stream().map(Chunk::content).toList());

        policyEmbeddingRepository.deleteAll();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            policyEmbeddingRepository.insert(
                    c.sourceType(), c.sourceRef(), c.crisisTags(), c.content(),
                    VectorLiterals.toLiteral(vectors.get(i)));
        }

        log.info("[PolicyIndexingService] reindexed {} policy chunk(s).", chunks.size());
        return chunks.size();
    }

    /** Wraps tags as {@code ,TAG1,TAG2,} so a {@code LIKE '%,TYPE,%'} filter matches whole tags. */
    private static String wrapTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return "," + String.join(",", tags) + ",";
    }

    /** Joins non-blank parts with newlines, trimming each. */
    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private record Chunk(String sourceType, String sourceRef, String crisisTags, String content) {
    }
}

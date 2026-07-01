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

    /** Target chunk size (chars) when splitting the rich welfare detail text. */
    static final int CHUNK_TARGET_SIZE = 500;

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
            String tags = wrapTags(w.getCrisisTags());

            // Prefer the rich detail text (상세조회 본문): split into ~500-char chunks so each
            // labeled section is embedded separately for finer retrieval. Fall back to the
            // short summary composite when no detail content is available.
            String detail = w.getDetailContent();
            if (detail != null && !detail.isBlank()) {
                List<String> pieces = chunkText(detail, CHUNK_TARGET_SIZE);
                for (int i = 0; i < pieces.size(); i++) {
                    chunks.add(new Chunk("WELFARE", w.getId() + "#" + i, tags, pieces.get(i)));
                }
                continue;
            }

            String content = joinNonBlank(
                    w.getServiceName(),
                    w.getSummary(),
                    w.getTargetDescription(),
                    w.getSelectionCriteria(),
                    w.getApplyMethod());
            if (content.isBlank()) {
                continue;
            }
            chunks.add(new Chunk("WELFARE", String.valueOf(w.getId()), tags, content));
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

    /**
     * Splits {@code text} into chunks of roughly {@code targetSize} characters, breaking on
     * line boundaries where possible. Lines are accumulated until adding the next one would
     * exceed {@code targetSize}, at which point the buffer is flushed. A single line longer
     * than {@code targetSize} becomes its own chunk. Deterministic and dependency-free.
     *
     * @return one chunk for short text, multiple for long text; never empty for non-blank input
     */
    static List<String> chunkText(String text, int targetSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        StringBuilder buf = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            // Flush before overflowing (only when the buffer already holds content).
            if (buf.length() > 0 && buf.length() + line.length() + 1 > targetSize) {
                chunks.add(buf.toString().trim());
                buf.setLength(0);
            }
            if (buf.length() > 0) {
                buf.append('\n');
            }
            buf.append(line);
        }
        if (buf.length() > 0 && !buf.toString().isBlank()) {
            chunks.add(buf.toString().trim());
        }
        return chunks;
    }

    private record Chunk(String sourceType, String sourceRef, String crisisTags, String content) {
    }
}

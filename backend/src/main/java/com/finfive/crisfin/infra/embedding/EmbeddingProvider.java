package com.finfive.crisfin.infra.embedding;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy interface for text-embedding provider integrations.
 *
 * <p>Mirrors the swappable-provider pattern used by {@code LlmProvider}: each concrete
 * implementation wraps a single embedding backend (e.g. OpenAI) and is registered as a
 * Spring bean. Callers must treat embeddings as an optional capability — when
 * {@link #isEnabled()} returns {@code false} (no API key), retrieval/indexing degrade
 * gracefully to a no-op and the surrounding feature keeps its original behaviour.</p>
 *
 * <p>Implementations must be stateless and thread-safe.</p>
 */
public interface EmbeddingProvider {

    /** Dimensionality of the vectors produced by this provider (e.g. 1536). */
    int dimensions();

    /** Whether the provider is usable (typically: an API key is configured). */
    boolean isEnabled();

    /**
     * Embeds a single piece of text into a dense vector.
     *
     * @throws IllegalStateException if the provider is not enabled
     * @throws RuntimeException      if the upstream embedding call fails
     */
    float[] embed(String text);

    /**
     * Embeds many texts, preserving input order in the returned list.
     *
     * <p>The default implementation embeds one-by-one; providers that support batch
     * requests should override this for efficiency.</p>
     */
    default List<float[]> embedAll(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) {
            out.add(embed(t));
        }
        return out;
    }
}

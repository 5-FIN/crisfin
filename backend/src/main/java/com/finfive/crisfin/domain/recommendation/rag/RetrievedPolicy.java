package com.finfive.crisfin.domain.recommendation.rag;

/**
 * A single policy chunk retrieved from the vector store.
 *
 * @param sourceType origin of the chunk, e.g. {@code "WELFARE"} or {@code "GUIDE"}
 * @param sourceRef  identifier within that source (typically the row id)
 * @param content    the retrieved text used as grounding context for the LLM
 * @param distance   cosine distance to the query vector (smaller = more similar)
 */
public record RetrievedPolicy(
        String sourceType,
        String sourceRef,
        String content,
        double distance
) {
}

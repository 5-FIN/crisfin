package com.finfive.crisfin.domain.recommendation.rag.dto;

/**
 * Result of a manual RAG reindex.
 *
 * @param indexedChunks number of policy chunks embedded and stored (0 when RAG is disabled
 *                      or there is no source data)
 */
public record ReindexResponse(int indexedChunks) {
}

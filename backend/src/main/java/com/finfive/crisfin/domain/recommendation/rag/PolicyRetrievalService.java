package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.infra.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side of the hybrid RAG pipeline: turns a free-text query into the most relevant
 * policy chunks from the vector store.
 *
 * <p>Degrades gracefully — returns an empty list (never throws) when embeddings are
 * disabled, the query is blank, or the upstream embedding/search call fails. This keeps the
 * LLM strategy step working exactly as before whenever RAG is unavailable.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PolicyRetrievalService {

    private final EmbeddingProvider embeddingProvider;
    private final PolicyEmbeddingRepository policyEmbeddingRepository;

    /**
     * Retrieves up to {@code topK} policy chunks relevant to {@code query}, optionally
     * filtered by {@code crisisType}.
     *
     * @return relevant chunks ordered by ascending distance, or an empty list when RAG is
     *         unavailable or fails
     */
    @Transactional(readOnly = true)
    public List<RetrievedPolicy> retrieve(CrisisType crisisType, String query, int topK) {
        if (!embeddingProvider.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            float[] queryVector = embeddingProvider.embed(query);
            String literal = VectorLiterals.toLiteral(queryVector);
            return policyEmbeddingRepository.searchNearest(literal, crisisType, topK);
        } catch (Exception e) {
            log.warn("[PolicyRetrievalService] retrieval failed, returning empty result: {}", e.getMessage());
            return List.of();
        }
    }
}

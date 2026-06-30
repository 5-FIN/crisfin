package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.recommendation.rag.dto.ReindexResponse;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin operations for the hybrid RAG index.
 *
 * <p>Reindexing embeds local policy sources (welfare benefits + crisis guides) into the
 * {@code policy_embeddings} store. It is a privileged, potentially costly operation (calls the
 * embedding provider), so it is restricted to {@code ADMIN} users.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/rag")
@RequiredArgsConstructor
public class RagAdminController {

    private final PolicyIndexingService policyIndexingService;

    /**
     * Rebuilds the RAG policy index on demand.
     *
     * <p>Returns the number of indexed chunks. {@code 0} means RAG is disabled (no
     * {@code OPENAI_API_KEY}) or there is no source data — the call still succeeds.</p>
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReindexResponse>> reindex() {
        int indexed = policyIndexingService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(new ReindexResponse(indexed)));
    }
}

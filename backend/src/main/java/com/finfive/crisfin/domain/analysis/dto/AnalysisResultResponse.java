package com.finfive.crisfin.domain.analysis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO returned after a successful LLM-powered analysis.
 *
 * <p>{@code result}       – Validated JSON tree produced by the LLM (contains todos, receivable,
 * holdable, actions, summary, disclaimer).</p>
 * <p>{@code llmProvider}  – Human-readable name of the LLM provider that fulfilled the request.</p>
 */
@Getter
@Builder
public class AnalysisResultResponse {

    private final Long id;
    private final String crisisType;
    private final String situationDescription;
    private final JsonNode result;
    private final String llmProvider;
    private final LocalDateTime createdAt;
}

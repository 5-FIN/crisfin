package com.finfive.crisfin.infra.llm.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic request object passed to any {@code LlmProvider} implementation.
 *
 * <ul>
 *   <li>{@code systemPrompt}   – Optional system / context instruction.</li>
 *   <li>{@code userMessage}    – The user turn content (required).</li>
 *   <li>{@code messages}       – Full conversation history when multi-turn context
 *       must be forwarded to the model.</li>
 *   <li>{@code maxTokens}      – Upper bound on the response length.</li>
 *   <li>{@code temperature}    – Sampling temperature (0.0–1.0).</li>
 *   <li>{@code extraParams}    – Provider-specific key/value overrides.</li>
 * </ul>
 */
@Getter
@Builder
public class LlmRequest {

    private final String systemPrompt;
    private final String userMessage;
    private final List<Map<String, String>> messages;

    @Builder.Default
    private final int maxTokens = 2048;

    @Builder.Default
    private final double temperature = 0.7;

    private final Map<String, Object> extraParams;
}

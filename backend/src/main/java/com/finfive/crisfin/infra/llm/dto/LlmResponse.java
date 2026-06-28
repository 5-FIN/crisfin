package com.finfive.crisfin.infra.llm.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Provider-agnostic response returned by any {@code LlmProvider} implementation.
 *
 * <ul>
 *   <li>{@code content}       – The model's generated text.</li>
 *   <li>{@code providerName}  – Identifier of the provider that fulfilled the request.</li>
 *   <li>{@code inputTokens}   – Number of tokens consumed in the prompt (may be 0 if
 *       the provider does not expose this).</li>
 *   <li>{@code outputTokens}  – Number of tokens generated in the completion.</li>
 *   <li>{@code finishReason}  – Stop condition reported by the provider
 *       (e.g. "stop", "length", "content_filter").</li>
 * </ul>
 */
@Getter
@Builder
public class LlmResponse {

    private final String content;
    private final String providerName;
    private final int inputTokens;
    private final int outputTokens;
    private final String finishReason;
}

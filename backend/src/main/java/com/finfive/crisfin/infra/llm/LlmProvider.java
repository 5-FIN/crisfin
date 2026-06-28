package com.finfive.crisfin.infra.llm;

import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;

/**
 * Strategy interface for LLM (Large Language Model) provider integrations.
 *
 * <p>Each concrete implementation wraps a single provider (e.g. OpenAI, Anthropic Claude,
 * Google Gemini) and is registered as a Spring bean. A fan-out / fallback service
 * iterates over all available providers in priority order and delegates to this interface.
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface LlmProvider {

    /**
     * Sends a completion request to the underlying LLM provider.
     *
     * @param request the provider-agnostic request descriptor
     * @return the provider's response
     * @throws com.finfive.crisfin.global.exception.CrisfinException with
     *         {@link com.finfive.crisfin.global.exception.ErrorCode#LLM_ALL_PROVIDERS_FAILED}
     *         if this provider cannot fulfil the request
     */
    LlmResponse complete(LlmRequest request);

    /**
     * Returns a stable, human-readable identifier for this provider
     * (e.g. {@code "openai-gpt4o"}, {@code "anthropic-claude-3-5-sonnet"}).
     */
    String getProviderName();
}

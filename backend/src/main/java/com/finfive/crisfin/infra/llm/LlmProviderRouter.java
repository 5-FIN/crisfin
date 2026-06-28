package com.finfive.crisfin.infra.llm;

import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LlmProviderRouter {

    private final List<LlmProvider> providers;

    @Value("${llm.providers.order:GEMINI,CLAUDE,OPENAI}")
    private String providerOrderStr;

    /**
     * Attempts each configured provider in priority order.
     * On exception, logs a warning and tries the next provider.
     * Throws {@link CrisfinException} with {@link ErrorCode#LLM_ALL_PROVIDERS_FAILED}
     * if every provider fails.
     */
    public LlmResponse complete(LlmRequest request) {
        Map<String, LlmProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(LlmProvider::getProviderName, Function.identity()));

        List<String> orderedNames = Arrays.stream(providerOrderStr.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();

        for (String name : orderedNames) {
            LlmProvider provider = providerMap.get(name);
            if (provider == null) {
                log.warn("[LlmProviderRouter] No provider registered for name '{}', skipping", name);
                continue;
            }
            try {
                log.info("[LlmProviderRouter] Trying provider '{}'", name);
                LlmResponse response = provider.complete(request);
                log.info("[LlmProviderRouter] Provider '{}' succeeded", name);
                return response;
            } catch (Exception e) {
                log.warn("[LlmProviderRouter] Provider '{}' failed: {}", name, e.getMessage());
            }
        }

        log.error("[LlmProviderRouter] All providers failed. Attempted order: {}", orderedNames);
        throw new CrisfinException(ErrorCode.LLM_ALL_PROVIDERS_FAILED);
    }
}

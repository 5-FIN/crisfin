package com.finfive.crisfin.infra.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClaudeProvider implements LlmProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${llm.claude.api-key:}")
    private String apiKey;

    @Value("${llm.claude.model:claude-3-5-sonnet-20241022}")
    private String model;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Claude API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", request.getSystemPrompt() != null ? request.getSystemPrompt() : "",
                "messages", List.of(
                        Map.of("role", "user", "content", request.getUserMessage())
                )
        );

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(ANTHROPIC_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("content").get(0).path("text").asText();

            int inputTokens = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);
            String finishReason = root.path("stop_reason").asText("stop");

            log.info("[ClaudeProvider] success — inputTokens={}, outputTokens={}", inputTokens, outputTokens);

            return LlmResponse.builder()
                    .content(content)
                    .providerName(getProviderName())
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .finishReason(finishReason)
                    .build();

        } catch (Exception e) {
            log.warn("[ClaudeProvider] request failed: {}", e.getMessage());
            throw new RuntimeException("Claude completion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "CLAUDE";
    }
}

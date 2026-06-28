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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiProvider implements LlmProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${llm.openai.api-key:}")
    private String apiKey;

    @Value("${llm.openai.model:gpt-4o}")
    private String model;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OpenAI API key is not configured");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage()));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(OPENAI_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            String finishReason = root.path("choices").get(0).path("finish_reason").asText("stop");

            log.info("[OpenAiProvider] success — promptTokens={}, completionTokens={}", promptTokens, completionTokens);

            return LlmResponse.builder()
                    .content(content)
                    .providerName(getProviderName())
                    .inputTokens(promptTokens)
                    .outputTokens(completionTokens)
                    .finishReason(finishReason)
                    .build();

        } catch (Exception e) {
            log.warn("[OpenAiProvider] request failed: {}", e.getMessage());
            throw new RuntimeException("OpenAI completion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }
}

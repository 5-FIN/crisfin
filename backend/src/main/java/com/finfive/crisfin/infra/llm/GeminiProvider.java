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
public class GeminiProvider implements LlmProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${llm.gemini.api-key:}")
    private String apiKey;

    @Value("${llm.gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API key is not configured");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        String combinedText = (request.getSystemPrompt() != null ? request.getSystemPrompt() + "\n\n" : "")
                + request.getUserMessage();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", combinedText))
                        )
                )
        );

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            int outputTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);
            int inputTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);

            log.info("[GeminiProvider] success — inputTokens={}, outputTokens={}", inputTokens, outputTokens);

            return LlmResponse.builder()
                    .content(content)
                    .providerName(getProviderName())
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .finishReason("stop")
                    .build();

        } catch (Exception e) {
            log.warn("[GeminiProvider] request failed: {}", e.getMessage());
            throw new RuntimeException("Gemini completion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }
}

package com.finfive.crisfin.infra.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OpenAI implementation of {@link EmbeddingProvider}.
 *
 * <p>Calls {@code POST https://api.openai.com/v1/embeddings} with the configured model
 * (default {@code text-embedding-3-small}, 1536 dimensions). The API key is injected from
 * the environment via {@code embedding.openai.api-key} ({@code ${OPENAI_API_KEY:}}); when
 * blank, {@link #isEnabled()} returns {@code false} and the RAG feature stays disabled.</p>
 *
 * <p>Follows the {@code WebClient.Builder} injection pattern of
 * {@link com.finfive.crisfin.infra.llm.ClaudeProvider}.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${embedding.openai.api-key:}")
    private String apiKey;

    @Value("${embedding.openai.model:text-embedding-3-small}")
    private String model;

    @Value("${embedding.openai.dimensions:1536}")
    private int dimensions;

    /**
     * Max inputs per embeddings request. OpenAI caps a single request at 2048 array items
     * (and ~300k tokens); we batch well under that so a large reindex (수천 청크) does not
     * exceed the per-request limit.
     */
    @Value("${embedding.openai.batch-size:200}")
    private int batchSize;

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (!isEnabled()) {
            throw new IllegalStateException("OpenAI embedding API key is not configured");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // Batch to stay under the OpenAI per-request limit (array/token). Batches are
        // embedded sequentially and concatenated in original order.
        int size = Math.max(1, batchSize);
        List<float[]> all = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += size) {
            int end = Math.min(start + size, texts.size());
            all.addAll(embedBatch(texts.subList(start, end)));
        }
        return all;
    }

    /** Embeds a single batch (≤ batchSize) in one API request, preserving request order. */
    private List<float[]> embedBatch(List<String> texts) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", texts
        );

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(EMBEDDINGS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode data = objectMapper.readTree(responseBody).path("data");

            // Restore vectors into request order using each element's "index" field.
            float[][] ordered = new float[texts.size()][];
            for (JsonNode item : data) {
                int idx = item.path("index").asInt();
                JsonNode embedding = item.path("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vec[i] = (float) embedding.get(i).asDouble();
                }
                ordered[idx] = vec;
            }

            log.info("[OpenAiEmbeddingProvider] embedded {} text(s) with model={}", texts.size(), model);
            return Arrays.asList(ordered);

        } catch (Exception e) {
            log.warn("[OpenAiEmbeddingProvider] embedding request failed: {}", e.getMessage());
            throw new RuntimeException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }
}

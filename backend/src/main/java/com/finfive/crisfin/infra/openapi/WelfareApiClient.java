package com.finfive.crisfin.infra.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class WelfareApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${welfare.api.key:}")
    private String apiKey;

    @Value("${welfare.api.base-url:https://api.odcloud.kr/api}")
    private String baseUrl;

    /**
     * Returns {@code true} when a non-blank API key is configured and live API
     * calls are expected to succeed.
     */
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * Fetches welfare benefit items from the public data portal.
     *
     * @param page     1-based page number
     * @param perPage  number of items per page
     * @return parsed {@link WelfareApiResponse}; returns an empty response when the API key is not set
     */
    public WelfareApiResponse getWelfareBenefits(int page, int perPage) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("공공API 키 미설정 — 복지 서비스 조회를 건너뜁니다.");
            return WelfareApiResponse.empty();
        }

        String uri = baseUrl
                + "/uddi:8ac7b1e5-ae7e-4dc4-a55c-d0e703c00e09"
                + "?serviceKey=" + apiKey
                + "&page=" + page
                + "&perPage=" + perPage
                + "&returnType=JSON";

        try {
            String responseBody = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(responseBody, WelfareApiResponse.class);

        } catch (Exception e) {
            log.error("[WelfareApiClient] 복지 서비스 API 호출 실패: {}", e.getMessage(), e);
            throw new CrisfinException(ErrorCode.WELFARE_API_UNAVAILABLE, e);
        }
    }
}

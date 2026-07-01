package com.finfive.crisfin.infra.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import com.finfive.crisfin.infra.openapi.dto.WelfareDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client for the LocalGovernment welfare service API (지자체복지서비스).
 *
 * <p>Endpoint: {@code GET {base-url}/LcgvWelfarelist}. The {@code serviceKey}
 * is a <em>decoded</em> key (may contain {@code +}, {@code /}, {@code =}) and
 * therefore must be encoded exactly once. This is achieved by passing it through
 * the {@link WebClient} {@code uriBuilder} query params (which URL-encode once)
 * rather than string concatenation. Responses are XML, parsed with
 * {@link XmlMapper}.
 */
@Component
@Slf4j
public class WelfareApiClient {

    private final WebClient.Builder webClientBuilder;
    private final XmlMapper xmlMapper;

    @Value("${welfare.api.key:}")
    private String apiKey;

    @Value("${welfare.api.base-url:http://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations}")
    private String baseUrl;

    public WelfareApiClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Returns {@code true} when a non-blank API key is configured and live API
     * calls are expected to succeed.
     */
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * Fetches welfare benefit items from the LocalGovernment welfare API.
     *
     * @param page    1-based page number
     * @param perPage number of items per page
     * @return parsed {@link WelfareApiResponse}; returns an empty response when the API key is not set
     */
    public WelfareApiResponse getWelfareBenefits(int page, int perPage) {
        if (!isConfigured()) {
            log.info("공공API 키 미설정 — 복지 서비스 조회를 건너뜁니다.");
            return WelfareApiResponse.empty();
        }

        try {
            // serviceKey는 decoded 키(+, /, = 포함)라 직접 퍼센트 인코딩해야 한다.
            // WebClient 기본 쿼리 인코딩은 '+'를 인코딩하지 않아(서버가 공백으로 해석) 401이 난다.
            // URLEncoder로 인코딩한 뒤 완성된 URI를 넘겨 WebClient가 재인코딩하지 않게 한다.
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            URI uri = URI.create(baseUrl + "/LcgvWelfarelist"
                    + "?serviceKey=" + encodedKey
                    + "&pageNo=" + page
                    + "&numOfRows=" + perPage);

            String responseBody = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return xmlMapper.readValue(responseBody, WelfareApiResponse.class);

        } catch (Exception e) {
            log.error("[WelfareApiClient] 복지 서비스 API 호출 실패: {}", e.getMessage(), e);
            throw new CrisfinException(ErrorCode.WELFARE_API_UNAVAILABLE, e);
        }
    }

    /**
     * Fetches the FULL detail of a single welfare service (지자체복지 상세조회) from the
     * {@code /LcgvWelfaredetailed} endpoint. Used to build the rich RAG ingest text.
     *
     * <p>Failure-tolerant by design: a single detail lookup must never abort the whole
     * sync, so any error is logged as a warning and {@code null} is returned instead of
     * throwing. Reuses the exact {@code serviceKey} encoding + {@link URI#create(String)}
     * approach as {@link #getWelfareBenefits(int, int)} so the '+' in the decoded key is
     * encoded exactly once.
     *
     * @param servId the service ID (from the list response's {@code servId})
     * @return parsed {@link WelfareDetailResponse}, or {@code null} when not configured or on any failure
     */
    public WelfareDetailResponse getWelfareDetail(String servId) {
        if (!isConfigured()) {
            return null;
        }

        // 목록 조회와 동일한 인코딩 방식(직접 퍼센트 인코딩 후 완성 URI 전달).
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "/LcgvWelfaredetailed"
                + "?serviceKey=" + encodedKey
                + "&servId=" + servId);

        // 429(Too Many Requests)는 일시적 스로틀일 수 있어 지수 백오프로 소폭 재시도한다.
        // 그 외 오류나 재시도 소진 시에는 전체 동기화를 막지 않도록 null을 반환한다.
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String responseBody = webClientBuilder.build()
                        .get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                return xmlMapper.readValue(responseBody, WelfareDetailResponse.class);
            } catch (WebClientResponseException.TooManyRequests e) {
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(500L * attempt); // 0.5s, 1.0s 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }
                log.warn("[WelfareApiClient] 복지 상세 429 (재시도 소진, servId={})", servId);
                return null;
            } catch (Exception e) {
                // 개별 상세 실패는 전체 동기화를 중단시키지 않는다 — 경고만 남기고 null 반환.
                log.warn("[WelfareApiClient] 복지 상세 조회 실패 (servId={}): {}", servId, e.getMessage());
                return null;
            }
        }
        return null;
    }
}

package com.finfive.crisfin.infra.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

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
}

package com.finfive.crisfin.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO carrying the public share token for an analysis result.
 *
 * <p>The token is unguessable; anyone holding it may view the result read-only
 * (via {@code GET /api/v1/analysis/shared/{token}}) without authentication.</p>
 */
@Getter
@Builder
public class ShareLinkResponse {

    private final String shareToken;

    public static ShareLinkResponse of(String token) {
        return ShareLinkResponse.builder()
                .shareToken(token)
                .build();
    }
}

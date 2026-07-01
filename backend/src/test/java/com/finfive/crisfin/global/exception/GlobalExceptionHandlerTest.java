package com.finfive.crisfin.global.exception;

import com.finfive.crisfin.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNoResourceFound_returns404NotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "api/v1/nope");

        ResponseEntity<ApiResponse<Void>> res = handler.handleNoResourceFound(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleCrisfinException_usesErrorCodeStatus() {
        CrisfinException ex = new CrisfinException(ErrorCode.PAYMENT_REQUIRED);

        ResponseEntity<ApiResponse<Void>> res = handler.handleCrisfinException(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(402);
        assertThat(res.getBody().getError().getCode()).isEqualTo("PAYMENT_REQUIRED");
    }
}

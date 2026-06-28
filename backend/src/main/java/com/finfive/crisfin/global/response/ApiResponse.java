package com.finfive.crisfin.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorInfo error;
    private final String message;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    // ------------------------------------------------------------------ //
    //  Success factories
    // ------------------------------------------------------------------ //

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <Void> ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Error factories
    // ------------------------------------------------------------------ //

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode))
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String detail) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode, detail))
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Inner classes
    // ------------------------------------------------------------------ //

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {

        private final String code;
        private final String message;
        private final String detail;

        public static ErrorInfo of(ErrorCode errorCode) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build();
        }

        public static ErrorInfo of(ErrorCode errorCode, String detail) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .detail(detail)
                    .build();
        }
    }
}

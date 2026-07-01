package com.finfive.crisfin.global.exception;

import com.finfive.crisfin.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Business exceptions raised anywhere in the application.
     */
    @ExceptionHandler(CrisfinException.class)
    public ResponseEntity<ApiResponse<Void>> handleCrisfinException(CrisfinException ex) {
        log.warn("[CrisfinException] code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Void> body = ApiResponse.error(errorCode, ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * Bean Validation failures (e.g. @Valid on request bodies).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("[ValidationException] detail={}", detail);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .error(ApiResponse.ErrorInfo.builder()
                        .code("VALIDATION_FAILED")
                        .message("요청 값이 올바르지 않습니다.")
                        .detail(detail)
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Unknown route / missing static resource → 404 (not a 500).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("[NotFound] resourcePath={}", ex.getResourcePath());
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .error(ApiResponse.ErrorInfo.builder()
                        .code("NOT_FOUND")
                        .message("요청한 리소스를 찾을 수 없습니다.")
                        .detail(ex.getResourcePath())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Catch-all for any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("[UnhandledException] {}", ex.getMessage(), ex);
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .error(ApiResponse.ErrorInfo.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message("서버 내부 오류가 발생했습니다.")
                        .detail(ex.getMessage())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

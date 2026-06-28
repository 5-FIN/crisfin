<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# common/exception

## Purpose
Global exception handling for the CrisFin REST API. Contains the `@RestControllerAdvice` that catches all unhandled exceptions and converts them to the standard `ApiResponse` error shape, plus custom exception types used across the application.

## For AI Agents

### Working In This Directory
- The global advice class should be the **single** place that converts exceptions to HTTP responses — do not add `@ExceptionHandler` methods to controllers
- Custom exceptions should be unchecked (`RuntimeException`) and carry an error code from a shared enum
- Map domain-level errors (e.g., "crisis not found") to appropriate HTTP status codes here, not in the service layer

### Common Patterns
```java
// Custom exception pattern
public class CrisfinException extends RuntimeException {
    private final ErrorCode errorCode;
}

// Global advice pattern
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CrisfinException.class)
    public ResponseEntity<ApiResponse<Void>> handleCrisfinException(CrisfinException e) { ... }
}
```

<!-- MANUAL: -->

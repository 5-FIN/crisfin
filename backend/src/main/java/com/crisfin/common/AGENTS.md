<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# common

## Purpose
Shared infrastructure used across all feature packages. Provides a standardized API response envelope and a global exception handling layer so every domain package returns consistent JSON shapes without repeating boilerplate.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `exception/` | Global `@RestControllerAdvice` and custom exception types (see `exception/AGENTS.md`) |
| `response/` | Generic `ApiResponse<T>` wrapper and status codes (see `response/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- Code here must have **zero** dependencies on other `com.crisfin.*` packages — it is the foundation layer
- When adding a new error type, add both the exception class in `exception/` and a matching `@ExceptionHandler` method in the global advice class
- Never add business logic here — only infrastructure utilities

### Common Patterns
- `ApiResponse<T>` wraps all successful payloads: `{ "success": true, "data": T }`
- Error responses follow: `{ "success": false, "code": "ERROR_CODE", "message": "..." }`
- Custom exceptions extend a base `CrisfinException` that carries an error code enum

### Testing Requirements
- Exception handler behavior should be covered by controller slice tests (`@WebMvcTest`) in the relevant domain test package

<!-- MANUAL: -->

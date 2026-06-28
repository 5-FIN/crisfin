<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# common/response

## Purpose
Standardized API response envelope used by every controller in the application. Ensures all endpoints return a consistent JSON structure regardless of domain.

## For AI Agents

### Working In This Directory
- `ApiResponse<T>` is the single generic wrapper — do not create domain-specific response wrappers at the top level
- Provide static factory methods (`ApiResponse.ok(data)`, `ApiResponse.error(code, message)`) so callers don't construct instances directly
- The wrapper must be serializable to JSON via Jackson with no extra configuration

### Common Patterns
```java
// Success shape
{ "success": true, "data": { ... } }

// Error shape
{ "success": false, "code": "USER_NOT_FOUND", "message": "해당 사용자를 찾을 수 없습니다." }

// Usage in controller
return ResponseEntity.ok(ApiResponse.ok(service.getResult()));
```

<!-- MANUAL: -->

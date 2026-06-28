<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# llm

## Purpose
LLM integration layer. Provides AI-generated personalized financial advice by sending structured prompts to an external LLM provider (e.g., OpenAI, Anthropic, or a Korean LLM service). Used by `guide/` and `recommendation/` to enrich static content with context-aware explanations.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | Request/response DTOs for LLM API calls (prompt request, completion response) |

## For AI Agents

### Working In This Directory
- This package is an **anti-corruption layer** — it hides the specific LLM provider behind a Java interface so callers don't couple to any vendor SDK
- LLM calls are inherently slow; consider making them async or caching results by crisis type + user profile hash
- Sensitive financial data should be scrubbed or anonymized before sending to an external LLM
- Store the raw LLM response in the DB for auditability and to avoid re-calling on page refresh

### Expected Files
| File | Purpose |
|------|---------|
| `LlmService.java` | Interface (or concrete class) for LLM calls |
| `LlmClient.java` | HTTP client wrapping the LLM provider API |
| `dto/LlmRequest.java` | Prompt + context sent to LLM |
| `dto/LlmResponse.java` | Parsed completion from LLM |

### Common Patterns
- Prompts should be externalized (in `resources/prompts/` or constants) — not hardcoded in service methods
- Use `spring-boot-starter-web`'s `RestTemplate` or `WebClient` for HTTP calls to the LLM API
- Wrap LLM timeouts/failures in a `CrisfinException` with a graceful fallback message

### Dependencies
| Package | Why |
|---------|-----|
| `config/` | API key injection via `@Value` or a config bean |
| `common/exception/` | LLM failure error codes |

<!-- MANUAL: -->

<!-- Parent: ../../../../../../backend/AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# com.crisfin (Application Source Root)

## Purpose
Root Java package for the CrisFin Spring Boot application. Contains all feature packages organized by domain. Each top-level package corresponds to a bounded context within the financial crisis guidance system.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `common/` | Shared infrastructure: exception handling, API response envelope (see `common/AGENTS.md`) |
| `config/` | Spring configuration beans: security, CORS, Swagger, JPA (see `config/AGENTS.md`) |
| `crisis/` | Core domain — crisis situation detection and classification (see `crisis/AGENTS.md`) |
| `guide/` | Financial guidance engine — step-by-step action plans (see `guide/AGENTS.md`) |
| `llm/` | LLM integration — AI-generated personalized advice (see `llm/AGENTS.md`) |
| `mydata/` | Korean MyData API client — financial account aggregation (see `mydata/AGENTS.md`) |
| `recommendation/` | Product and welfare recommendation engine (see `recommendation/AGENTS.md`) |
| `welfare/` | Welfare benefit catalog and eligibility matching (see `welfare/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- Place only the Spring Boot main application class here (`CrisfinApplication.java`) — nothing else belongs at this level
- All feature code goes into a dedicated sub-package (see Subdirectories above)
- Do not create cross-package dependencies that bypass the `common/` layer
- Package naming: `com.crisfin.<domain>` for controllers/services/repos, `com.crisfin.<domain>.dto` for request/response DTOs

### Common Patterns
- Controller → Service → Repository layering within each package
- DTOs live in `<domain>/dto/` sub-package
- Entities live directly in the domain package
- QueryDSL Q-types are auto-generated — never write them manually

### Testing Requirements
- Integration tests in `src/test/java/com/crisfin/<domain>/`
- Mirror the production package structure in test source tree

<!-- MANUAL: -->

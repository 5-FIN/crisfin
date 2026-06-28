<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# backend

## Purpose
The Spring Boot 3 REST API for CrisFin. Handles user authentication (JWT + Spring Security), financial data ingestion via MyData, LLM-powered guidance generation, welfare benefit matching, and product/service recommendations. Built with Java 21 and backed by PostgreSQL with QueryDSL for type-safe queries.

## Key Files
| File | Description |
|------|-------------|
| `build.gradle` | All dependencies: Spring Boot 3.3.4, JPA, Security, JWT, QueryDSL 5.1, Springdoc OpenAPI 2.6 |
| `settings.gradle` | Project name `crisfin-backend` |
| `gradlew` / `gradlew.bat` | Gradle wrapper (use `./gradlew` — do not require a system Gradle) |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `src/main/java/com/crisfin/` | Application source root (see `src/main/java/com/crisfin/AGENTS.md`) |
| `src/main/resources/` | Spring configuration files (`application.yml`, static resources) |
| `src/test/java/com/crisfin/` | Integration and unit tests (see `src/test/java/com/crisfin/AGENTS.md`) |
| `gradle/wrapper/` | Gradle wrapper JAR and properties — do not edit manually |

## For AI Agents

### Working In This Directory
- Always use `./gradlew` (never require global Gradle)
- Java 21 is required — use the toolchain declaration, don't hardcode JAVA_HOME
- QueryDSL Q-classes are generated into `build/generated/querydsl/` — they are NOT committed to git and must be regenerated after entity changes via `./gradlew compileJava`
- Springdoc UI is available at `/swagger-ui.html` when running locally

### Testing Requirements
- Run tests: `./gradlew test`
- Tests use H2 in-memory DB (configured separately from production `application.yml`)
- JUnit 5 via `useJUnitPlatform()`

### Common Patterns
- Standard Spring layering: Controller → Service → Repository
- All REST responses wrapped in the `common/response` envelope
- Exceptions handled globally via `common/exception`
- Security: JWT bearer tokens, configured in `config/`

## Dependencies

### Internal
- `src/main/java/com/crisfin/` — all application source packages

### External
- `spring-boot-starter-web` — REST endpoints
- `spring-boot-starter-data-jpa` — persistence (Hibernate + PostgreSQL)
- `spring-boot-starter-security` — authentication / authorization
- `spring-boot-starter-validation` — Bean Validation (Jakarta)
- `jjwt 0.12.6` — JWT creation and verification
- `springdoc-openapi 2.6.0` — Swagger UI / OpenAPI 3 docs
- `querydsl-jpa 5.1.0` — type-safe JPQL queries
- `postgresql` — production JDBC driver
- `h2` — in-memory DB for tests

<!-- MANUAL: -->

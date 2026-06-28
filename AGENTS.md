<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# crisfin

## Purpose
CrisFin (위기 상황 금융 길라잡이) is an MVP platform that provides personalized financial guidance and welfare benefit recommendations to individuals facing financial crisis situations. It integrates Korean MyData (open banking), LLM-powered advice, and a structured guide engine into a single REST API backend.

## Key Files
| File | Description |
|------|-------------|
| `backend/build.gradle` | Gradle build configuration — dependencies, QueryDSL setup, Java 21 toolchain |
| `backend/settings.gradle` | Root project name (`crisfin-backend`) |
| `backend/gradlew` | Gradle wrapper launcher (Unix) |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `backend/` | Spring Boot 3 REST API (see `backend/AGENTS.md`) |
| `frontend/` | Frontend client — not yet implemented (see `frontend/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- The root contains no application code — work happens inside `backend/` and `frontend/`
- Keep root-level files minimal (README, .gitignore, docker-compose, CI configs belong here)
- The project group is `com.crisfin`, version `0.0.1-SNAPSHOT`

### Testing Requirements
- Backend tests: `cd backend && ./gradlew test`
- H2 in-memory DB is used for tests; PostgreSQL is used for production

### Common Patterns
- Korean domain concepts: 위기(crisis), 복지(welfare), 마이데이터(MyData), 추천(recommendation)
- All API responses use a standardized wrapper from `common/response`

## Dependencies

### External Services
- Korean MyData API — open banking / financial account aggregation
- LLM provider — personalized financial advice generation
- PostgreSQL — primary data store

<!-- MANUAL: -->

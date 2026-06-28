<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# crisis

## Purpose
Core domain package for crisis situation detection and classification. A "crisis" in CrisFin refers to a financial distress event (e.g., job loss, sudden debt, medical expense, business closure). This package handles crisis intake, categorization, and persistence — it is the entry point of the user journey.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | Request/response DTOs for crisis-related API endpoints |

## For AI Agents

### Working In This Directory
- Crisis is the **root aggregate** — `guide`, `recommendation`, and `welfare` all reference a crisis ID
- The crisis entity should capture: type (enum), severity, user reference, created timestamp, and status
- Controller routes: `POST /api/crisis` (register), `GET /api/crisis/{id}` (detail), `GET /api/crisis/me` (my crises)

### Expected Files
| File | Purpose |
|------|---------|
| `Crisis.java` | JPA entity — the root aggregate |
| `CrisisType.java` | Enum: `JOB_LOSS`, `MEDICAL`, `DEBT`, `BUSINESS_CLOSURE`, etc. |
| `CrisisStatus.java` | Enum: `ACTIVE`, `RESOLVED`, `IN_PROGRESS` |
| `CrisisController.java` | REST endpoints under `/api/crisis` |
| `CrisisService.java` | Business logic for crisis registration and retrieval |
| `CrisisRepository.java` | Spring Data JPA repository |
| `dto/CrisisCreateRequest.java` | Intake form DTO with Bean Validation |
| `dto/CrisisResponse.java` | API response DTO |

### Common Patterns
- Use a `@CreatedDate` audit field (JPA Auditing) on the entity
- Return `CrisisResponse` DTOs, never expose the entity directly in API responses
- After crisis creation, trigger downstream processing (guide generation, recommendation) via an application event or direct service call

### Testing Requirements
- Controller: `@WebMvcTest(CrisisController.class)` with mocked service
- Service: unit test with mocked repository
- Repository: `@DataJpaTest` with H2

<!-- MANUAL: -->

<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# guide

## Purpose
Financial guidance engine that generates step-by-step action plans for users in crisis. Given a crisis type and user profile, this package produces ordered guidance steps (e.g., "contact creditors within 3 days", "apply for emergency fund") that lead the user through the situation.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | Request/response DTOs for guide-related endpoints |

## For AI Agents

### Working In This Directory
- A `Guide` is linked to a `Crisis` (one crisis can have one active guide)
- Guide steps are ordered and can have status: `PENDING`, `IN_PROGRESS`, `DONE`
- This package may call `llm/` to enrich steps with personalized LLM content
- Controller routes: `GET /api/guide/{crisisId}` (fetch guide for crisis), `PATCH /api/guide/step/{stepId}` (update step status)

### Expected Files
| File | Purpose |
|------|---------|
| `Guide.java` | JPA entity linked to Crisis |
| `GuideStep.java` | JPA entity for individual steps within a guide |
| `GuideController.java` | REST endpoints under `/api/guide` |
| `GuideService.java` | Guide generation and step management logic |
| `GuideRepository.java` | JPA repository for Guide |
| `dto/GuideResponse.java` | Full guide with ordered steps |
| `dto/StepStatusUpdateRequest.java` | DTO for updating step status |

### Common Patterns
- Guide generation is triggered after crisis creation — either synchronously (simple) or via an event
- Steps are ordered by a numeric `order` field on `GuideStep`
- LLM enrichment (if used) is called from `GuideService` via `llm.LlmService`

### Dependencies
| Package | Why |
|---------|-----|
| `crisis/` | Foreign key from Guide to Crisis |
| `llm/` | Optional: LLM-generated personalized step descriptions |

<!-- MANUAL: -->

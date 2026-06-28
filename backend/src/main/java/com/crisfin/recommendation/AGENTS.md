<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# recommendation

## Purpose
Recommendation engine that matches users in crisis with relevant financial products (low-interest emergency loans, credit restructuring programs) and welfare benefits based on their crisis type, financial profile (from MyData), and eligibility criteria. Produces a ranked, deduplicated list of actionable recommendations.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | Request/response DTOs for recommendation endpoints |

## For AI Agents

### Working In This Directory
- Recommendations span two categories: **financial products** (from partner institutions) and **welfare programs** (from `welfare/`)
- Eligibility rules can be hard-coded (MVP) or rule-engine-driven — start simple
- Controller routes: `GET /api/recommendation/{crisisId}` (get recommendations for a crisis)
- Recommendations should be re-computed when the user's financial profile changes

### Expected Files
| File | Purpose |
|------|---------|
| `RecommendationService.java` | Eligibility evaluation + ranking logic |
| `RecommendationController.java` | REST endpoints under `/api/recommendation` |
| `Recommendation.java` | Entity storing a single recommendation linked to a crisis |
| `RecommendationRepository.java` | JPA repository |
| `dto/RecommendationResponse.java` | Ranked list of recommendations with metadata |

### Common Patterns
- Each recommendation has a `type` (PRODUCT / WELFARE), `title`, `description`, `eligibilityReason`, and `priority` score
- Call `welfare/WelfareService` to get matching welfare programs
- Call `mydata/MyDataService` (or use cached financial profile) for eligibility input
- LLM enrichment (optional): use `llm/` to generate a personalized explanation per recommendation

### Dependencies
| Package | Why |
|---------|-----|
| `crisis/` | Crisis type drives which recommendations are relevant |
| `mydata/` | Financial profile for eligibility checking |
| `welfare/` | Welfare program catalog and matching |
| `llm/` | Optional: personalized recommendation explanations |

<!-- MANUAL: -->

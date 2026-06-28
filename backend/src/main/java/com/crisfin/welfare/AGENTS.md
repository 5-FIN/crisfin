<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# welfare

## Purpose
Welfare benefit catalog and eligibility matching. Maintains a database of Korean government and public welfare programs (긴급복지지원, 국민기초생활보장, 근로장려금, etc.) with their eligibility criteria, and provides a matching service that filters them based on a user's income, household size, crisis type, and other attributes.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | Request/response DTOs for welfare-related endpoints |

## For AI Agents

### Working In This Directory
- Welfare programs are relatively static data — seed them via a DB migration or `data.sql`, not hard-coded in service logic
- Eligibility rules are structured criteria (income threshold, age range, household size) — store them as structured fields on the `WelfareProgram` entity so they can be queried
- Controller routes: `GET /api/welfare` (list all programs), `GET /api/welfare/eligible?crisisId={id}` (eligible programs for a crisis)

### Expected Files
| File | Purpose |
|------|---------|
| `WelfareProgram.java` | JPA entity for a welfare program with eligibility fields |
| `WelfareProgramRepository.java` | JPA repository with QueryDSL for eligibility filtering |
| `WelfareService.java` | Matching logic — filters programs by user attributes |
| `WelfareController.java` | REST endpoints under `/api/welfare` |
| `dto/WelfareProgramResponse.java` | Program details + eligibility status DTO |

### Common Patterns
- Eligibility check: `WelfareService.findEligible(FinancialProfile, CrisisType)` → `List<WelfareProgram>`
- Use QueryDSL for complex eligibility predicate composition (income range, household size range)
- Programs should include `applicationUrl` or `contactInfo` so users can act on them directly

### Dependencies
| Package | Why |
|---------|-----|
| `mydata/` | Financial profile for income/asset eligibility checks |
| `crisis/` | Crisis type narrows the relevant program set |
| `recommendation/` | Called by recommendation engine to get welfare matches |

<!-- MANUAL: -->

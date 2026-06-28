<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# mydata

## Purpose
Korean MyData (마이데이터) integration package. MyData is the Korean open banking standard that lets users consent to sharing financial account data (bank accounts, loans, cards, insurance, pension) with third-party services. This package handles OAuth consent flow, data fetching from MyData operators, and normalization of the aggregated financial profile used for crisis assessment and recommendations.

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `dto/` | DTOs for MyData API responses and normalized internal financial profiles |

## For AI Agents

### Working In This Directory
- MyData requires **user consent** — never fetch financial data without a stored consent token
- Access tokens from MyData operators are short-lived; implement token refresh logic
- Financial data received from MyData should be stored locally (cached) to avoid repeated API calls and to allow offline analysis
- Respect Korean financial data regulations: data must not be used beyond the consented purpose

### Expected Files
| File | Purpose |
|------|---------|
| `MyDataService.java` | Orchestrates consent flow and data aggregation |
| `MyDataClient.java` | HTTP client for MyData API calls |
| `FinancialProfile.java` | Internal entity/DTO representing a user's normalized financial picture |
| `dto/MyDataConsentRequest.java` | Consent initiation DTO |
| `dto/AccountSummary.java` | Normalized account data from MyData |

### Common Patterns
- Store MyData access tokens encrypted in the DB, not in plain text
- The financial profile should be a snapshot with a `fetchedAt` timestamp — stale profiles should trigger a refresh
- Use this data to populate crisis severity scoring and recommendation eligibility checks

### Dependencies
| Package | Why |
|---------|-----|
| `config/` | MyData API endpoint and credentials |
| `recommendation/` | Financial profile feeds eligibility checks |
| `welfare/` | Income/asset data needed for welfare eligibility |

<!-- MANUAL: -->

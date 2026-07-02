# CrisFin — 위기 상황 금융 길라잡이

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot)
![Next.js](https://img.shields.io/badge/Next.js-15-000000?logo=nextdotjs)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16_+_pgvector-4169E1?logo=postgresql)

---

## 프로젝트 소개

갑작스러운 **입원·사고·실직·간병·사망** 같은 위기 상황이 닥치면, 사람들은 자신이 받을 수 있는 보험금·환급금·정부 지원금이 무엇인지 몰라 홀로 경제적 어려움을 감당합니다. 대한민국에는 11조 원이 넘는 미청구 보험금이 잠들어 있습니다.

**CrisFin**은 위기 상황에서 "지금 당장 뭘 해야 하는지"를 알려주는 2티어 서비스입니다.

- **무료 정보 길라잡이** — 로그인 없이 위기 유형별 핵심 정보와 "내 AI에 붙여넣을 프롬프트"를 제공.
- **유료 AI 맞춤 분석** — 마이데이터·상황을 입력하면 LLM + 룰 엔진이 **할 일 · 받을 돈 · 미룰 것 · 행동** 4가지로 정리하고, 내 지역 맞춤 복지 제도까지 추천.

> 금액은 **룰 엔진**이 산정하며, LLM은 전략 텍스트만 생성합니다(금액 환각 방지). 정책 근거는 **pgvector RAG**로 보강합니다.

---

## 핵심 기능

| 기능 | 설명 |
|------|------|
| **무료 길라잡이** | 5개 위기 유형별 핵심 수칙·근거 법령 + 복사용 AI 프롬프트 (로그인 불필요) |
| **AI 맞춤 분석** | 위기 유형 → 상황·마이데이터 입력 → LLM 전략 + 룰 엔진 금액산출 + RAG 근거 → 4분면 결과 |
| **대시보드** | 할 일·받을 돈·미룰 것·행동 4분면 + 재정 생존 기간 추산 + 30일 긴급도 타임라인 |
| **복지 매처** | 공공데이터 지자체복지 연동. **지역(시/도·시/군/구) + 위기유형 태그**로 필터, 상세 페이지, 즐겨찾기 |
| **개인화 재분석** | 상황·재정 정보 일부만 바꿔 다시 추론(reinfer) |
| **분석결과 공유** | 추측 불가 토큰 링크로 결과를 로그인 없이 공유(읽기 전용) |
| **요금제/결제** | 무료 / 1회 분석권 / 30일 무제한 3플랜, mock 결제 + 이용권 만료·사용횟수 게이팅 |
| **계정** | 회원가입(지역 포함) · 프로필/지역 수정 · 히스토리 · 가입 직후 온보딩 |
| **보안** | JWT(access/refresh 회전, jti) · 로그인 실패 잠금 · 분석결과 소유권 검증(IDOR 방지) |

---

## 서비스 흐름

```
①  위기 선택          ②  상황·마이데이터        ③  AI 분석                ④  4분면 결과
────────────      ────────────────────     ─────────────────      ──────────────────────
입원 / 사고 /      직업·상황 설명 +          LLM 전략 +              ✅ 할 일   💰 받을 돈
실직 / 간병 /      마이데이터(mock)          룰 엔진 금액산출 +       ⏸️ 미룰 것  📋 행동
사망 중 선택       필드 선택 입력            pgvector RAG 근거        + 내 지역 맞춤 복지
```

---

## 아키텍처

모노레포 (`backend/` + `frontend/`), 3-tier 구성.

```
[ 브라우저 ]
     │  (same-origin)
┌────▼─────────────┐   /api/* 서버사이드 프록시    ┌──────────────────────┐
│  frontend (Next) │ ───────────────────────────▶ │  backend (Spring Boot) │
│  Vercel          │        (BACKEND_URL)          │  Render (Docker)       │
└──────────────────┘                               └───────────┬────────────┘
                                                    JPA/Flyway  │  WebClient
                                          ┌─────────────────────▼──────┐   ┌──────────────┐
                                          │ PostgreSQL 16 + pgvector    │   │ OpenAI / LLM  │
                                          │ Neon (배포) / Docker (로컬)  │   │ 공공데이터포털 │
                                          └────────────────────────────┘   └──────────────┘
```

- 프론트는 `next.config.ts` rewrites로 `/api/*`를 `BACKEND_URL`로 프록시 → 브라우저는 same-origin, CORS 부담 없음.
- 인증은 **JWT + Authorization 헤더**(localStorage) 방식(쿠키 미사용).

---

## 기술 스택

### Backend (`backend/`)
| 분류 | 기술 |
|------|------|
| Language / Framework | Java 21 · Spring Boot 3.3 |
| 영속성 | Spring Data JPA / Hibernate · Flyway 마이그레이션 |
| 보안 | Spring Security + JWT (jjwt) |
| 외부 연동 | Spring WebFlux `WebClient` (LLM · 공공데이터 API) |
| AI / RAG | OpenAI Embeddings + pgvector 유사도 검색 · LLM 라우터(Gemini→Claude→OpenAI 폴백) |
| API 문서 | springdoc-openapi (Swagger UI) |

### Frontend (`frontend/`)
| 분류 | 기술 |
|------|------|
| Framework | Next.js 15 (App Router) · React 18 · TypeScript |
| 스타일 | Tailwind CSS |
| 테스트 | Vitest + Testing Library |

### Database / Infra
| 분류 | 기술 |
|------|------|
| DB | PostgreSQL 16 + **pgvector** |
| Container | Docker / Docker Compose |
| CI | GitHub Actions (JaCoCo 백엔드 + Vitest 프론트 커버리지) |
| 배포 | **Vercel**(프론트) + **Render**(백엔드 Docker) + **Neon**(DB) — [DEPLOY.md](DEPLOY.md) |

---

## 로컬 실행

### Prerequisites
- Docker & Docker Compose (Postgres+pgvector 구동)
- Java 21, Gradle 8.x (백엔드)
- Node.js 20+ (프론트)
- (선택) `OPENAI_API_KEY` — 없으면 RAG/유료 분석은 자동 비활성화되고 나머지는 정상 동작
- (선택) `PUBLIC_DATA_API_KEY` — 공공데이터포털 지자체복지 API 키(복지 동기화용)

### 1) DB (Docker)
```bash
docker compose up -d      # pgvector/pgvector:pg16, DB/USER/PW = crisfin/crisfin/crisfin
```

### 2) 백엔드
```bash
# 환경변수(예): DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, OPENAI_API_KEY ...
cd backend && ./gradlew bootRun      # http://localhost:8080
```
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3) 프론트
```bash
cd frontend && npm install && npm run dev      # http://localhost:3000
```
> 프론트는 `/api/*`를 `BACKEND_URL`(기본 `http://localhost:8080`)로 프록시합니다.

### 주요 환경변수
| Key | 용도 |
|-----|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres 접속 |
| `JWT_SECRET` | JWT 서명 키(256bit+) |
| `OPENAI_API_KEY` | 임베딩(RAG) + LLM 분석 |
| `LLM_PROVIDERS_ORDER` | LLM 폴백 순서(예: `OPENAI`) |
| `PUBLIC_DATA_API_KEY` | 공공데이터 지자체복지 API |
| `CORS_ALLOWED_ORIGINS` | 허용 오리진(프록시 구조라 대개 불요) |
| `WELFARE_SYNC_ON_STARTUP` | 빈 DB 첫 부팅 자동 복지 동기화(기본 true; 무료 인스턴스는 false 권장) |
| `PORT` | 백엔드 바인딩 포트(Render 등에서 주입) |
| `BACKEND_URL` | (프론트) 백엔드 프록시 대상 |

---

## API 엔드포인트 (요약)

`공개` = 인증 불필요, `로그인` = JWT 필요, `유료` = 로그인 + 이용권, `관리자` = ROLE_ADMIN.

| Method | URL | 접근 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/v1/auth/signup` · `/login` · `/refresh` · `/logout` | 공개 | 회원가입/로그인/토큰갱신/로그아웃 |
| `GET` `PUT` | `/api/v1/users/me` | 로그인 | 내 프로필 조회/수정(닉네임·지역) |
| `GET` | `/api/v1/crisis/types` | 공개 | 위기 유형 목록 |
| `GET` | `/api/v1/guide/{crisisType}` | 공개 | 무료 길라잡이(위기별 정보 + 프롬프트) |
| `POST` | `/api/v1/analysis/recommend` | 유료 | AI 맞춤 분석 실행 |
| `POST` | `/api/v1/analysis/{id}/reinfer` | 유료 | 개인화 재분석 |
| `GET` | `/api/v1/analysis/history` · `/results/{id}` | 로그인 | 히스토리 / 결과 조회(소유자만) |
| `POST` `DELETE` | `/api/v1/analysis/{id}/share` | 로그인 | 공유 링크 생성/해제 |
| `GET` | `/api/v1/analysis/shared/{token}` | 공개 | 공유 결과 조회(토큰) |
| `GET` | `/api/v1/welfare/benefits` · `/benefits/{id}` | 공개 | 복지 목록(지역·위기 필터) / 상세 |
| `GET` `POST` `DELETE` | `/api/v1/welfare/favorites` · `/{id}` | 로그인 | 즐겨찾기 목록/추가/해제 |
| `GET` `POST` | `/api/v1/payments/plans` · `/checkout` · `/confirm` · `/entitlement` | 로그인 | 요금제/결제(mock)/이용권 |
| `GET` `POST` | `/api/v1/mydata/mock` · `/filter` | 공개 | 마이데이터(mock) 조회/필드 선택 |
| `POST` | `/api/v1/admin/welfare/sync` · `/admin/rag/reindex` | 관리자 | 복지 동기화 / RAG 재색인 |

전체 상세 명세: [docs/API.md](docs/API.md) · 백엔드 구조: [docs/BACKEND.md](docs/BACKEND.md)

---

## 문서
- [DEPLOY.md](DEPLOY.md) — 무료 배포 가이드(Vercel + Render + Neon)
- [docs/API.md](docs/API.md) — API 상세 명세
- [docs/BACKEND.md](docs/BACKEND.md) — 백엔드 아키텍처
- [docs/CONVENTION.md](docs/CONVENTION.md) — 브랜치 전략 / 커밋 컨벤션

---

## 팀

**FIN5 (5-FIN)** — 위기 금융 서비스

| 역할 | 담당 |
|------|------|
| PM / Backend | 팀원 1 |
| Backend | 팀원 2 |
| Backend | 팀원 3 |
| Frontend | 팀원 4 |
| Frontend | 팀원 5 |

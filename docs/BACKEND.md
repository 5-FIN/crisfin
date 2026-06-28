# CrisFin 백엔드 기능 명세 및 플로우

> **패키지** `com.finfive.crisfin` · **Java 21** · **Spring Boot 3.3.4**

---

## 목차

1. [구현 기능 목록](#1-구현-기능-목록)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [핵심 플로우](#3-핵심-플로우)
   - 3-1. 회원가입 / 로그인
   - 3-2. AI 분석 (핵심 기능)
   - 3-3. 마이데이터 선택 → 분석 전달
   - 3-4. LLM 폴백 체인
   - 3-5. 복지 DB 배치 동기화
4. [보안 레이어](#4-보안-레이어)
5. [데이터베이스 구조 요약](#5-데이터베이스-구조-요약)

---

## 1. 구현 기능 목록

### 도메인 기능

| # | 도메인 | 기능 | 인증 필요 |
|---|--------|------|-----------|
| 1 | **Crisis** | 위기 유형 5종 목록 조회 (HOSPITALIZATION·ACCIDENT·UNEMPLOYMENT·CAREGIVING·BEREAVEMENT) | ❌ |
| 2 | **Guide** | 위기 유형별 무료 코칭 프롬프트 + 핵심 수칙 제공 | ❌ |
| 3 | **MyData** | 5개 페르소나 Mock 금융 데이터 조회 | ❌ |
| 3 | **MyData** | 원하는 필드만 선택해 필터링 (카드/대출/보험/자동이체 등) | ❌ |
| 4 | **Analysis** | LLM 기반 위기 금융 분석 — 할일·받을돈·미룰것·행동 4가지 출력 | ❌ (선택적) |
| 4 | **Analysis** | 분석 결과 ID로 재조회 (공유 링크용) | ❌ |
| 5 | **Welfare** | 복지 제도 목록 페이지네이션 조회 + 위기 유형 필터 | ❌ |
| 5 | **Welfare** | 복지 제도 단건 상세 조회 | ❌ |
| 6 | **User/Auth** | 회원가입, 로그인, 토큰 갱신, 로그아웃 | ❌ |

### 인프라 기능

| 기능 | 설명 |
|------|------|
| **JWT 인증** | Access Token 30분 / Refresh Token 7일, Refresh Token Rotation |
| **LLM 폴백 체인** | Gemini → Claude → OpenAI 순서 자동 폴백 |
| **PII 마스킹** | LLM 전송 전 계좌번호·카드번호·주민번호·실명 자동 치환 |
| **프롬프트 인젝션 탐지** | "ignore previous instructions" 류 패턴 감지 → 400 반환 |
| **LLM 응답 검증** | JSON 파싱 실패 시 최대 2회 재시도 |
| **복지 배치 동기화** | 매일 새벽 3시 공공 API → welfare_benefits 테이블 upsert |
| **CORS** | `CORS_ALLOWED_ORIGINS` 환경변수로 허용 오리진 설정 |
| **Swagger UI** | `/swagger-ui.html` 자동 API 문서 |
| **Flyway 마이그레이션** | V1 스키마 생성 → V2 가이드 시드 → V3 마이데이터 시드 → V4 복지 시드 |

---

## 2. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
│              (React / Next.js / 모바일)                  │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP + JWT Bearer
┌──────────────────────▼──────────────────────────────────┐
│               Spring Security Filter Chain              │
│  JwtAuthenticationFilter → PromptInjectionDetector      │
│  PiiMaskingService → LlmResponseValidator               │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┼──────────────────┐
        ▼              ▼                  ▼
  ┌──────────┐  ┌────────────┐    ┌─────────────┐
  │  Auth /  │  │  Domain    │    │  Analysis   │
  │  User    │  │  (Crisis / │    │  Service    │
  │ Service  │  │  Guide /   │    │  (핵심)     │
  └────┬─────┘  │  MyData /  │    └──────┬──────┘
       │        │  Welfare)  │           │
       │        └────────────┘           ▼
       │                        ┌─────────────────┐
       │                        │ LlmProviderRouter│
       │                        │ Gemini → Claude  │
       │                        │ → OpenAI → Mock  │
       │                        └─────────────────┘
       ▼
┌─────────────┐           ┌─────────────────────┐
│  PostgreSQL  │           │   공공 API (복지로)  │
│  (Flyway)   │           │   WelfareApiClient  │
└─────────────┘           └─────────────────────┘
```

---

## 3. 핵심 플로우

### 3-1. 회원가입 / 로그인

```
[Client]                    [AuthController]          [UserService]           [DB]
   │                               │                       │                   │
   │── POST /auth/signup ─────────▶│                       │                   │
   │   { email, password,          │── signup(req) ───────▶│                   │
   │     nickname }                │                       │── existsByEmail? ─▶│
   │                               │                       │◀─ false ──────────│
   │                               │                       │── save(User) ────▶│
   │                               │                       │── save(RefreshToken)▶│
   │◀── { accessToken,             │◀── AuthResponse ──────│                   │
   │      refreshToken }           │                       │                   │

[토큰 만료 후 갱신]
   │── POST /auth/refresh ────────▶│                       │                   │
   │   { refreshToken }            │── refresh(token) ────▶│                   │
   │                               │                       │── findByToken ───▶│
   │                               │                       │── 기존 삭제        │
   │                               │                       │── 새 토큰 쌍 발급  │
   │◀── { 새 accessToken,          │◀── AuthResponse ──────│                   │
   │      새 refreshToken }        │    (Rotation 완료)     │                   │
```

---

### 3-2. AI 분석 (핵심 기능)

```
[Client]           [AnalysisController]    [AnalysisService]       [LlmProviderRouter]    [DB]
   │                      │                      │                         │               │
   │─ POST /analysis ────▶│                      │                         │               │
   │  /recommend          │── recommend(req) ───▶│                         │               │
   │  {                   │                      │─① 프롬프트 인젝션 탐지   │               │
   │   crisisType,        │                      │  (situationDescription) │               │
   │   situation,         │                      │─② PII 마스킹            │               │
   │   filteredMyData     │                      │  (filteredMyData)       │               │
   │  }                   │                      │─③ 시스템 프롬프트 생성   │               │
   │                      │                      │  (위기유형별 제도 지식)  │               │
   │                      │                      │─④ LLM 호출 ────────────▶│               │
   │                      │                      │                         │─ Gemini 시도  │
   │                      │                      │                         │  (실패 시)    │
   │                      │                      │                         │─ Claude 시도  │
   │                      │                      │                         │  (실패 시)    │
   │                      │                      │                         │─ OpenAI 시도  │
   │                      │                      │◀── LlmResponse ─────────│               │
   │                      │                      │─⑤ JSON 응답 검증        │               │
   │                      │                      │  (파싱 실패 시 재시도)  │               │
   │                      │                      │─⑥ DB 저장 ─────────────────────────────▶│
   │◀── ApiResponse ──────│◀── AnalysisResult ───│                         │               │
   │  {                   │                      │                         │               │
   │   todos,             │                      │
   │   receivable,        │
   │   holdable,          │
   │   actions,           │
   │   summary            │
   │  }                   │
```

---

### 3-3. 마이데이터 선택 → 분석 전달 (권장 UX 플로우)

```
① 페르소나 선택
   GET /api/v1/mydata/mock?persona=OFFICE_WORKER
   → 전체 금융 데이터 미리보기

② 원하는 항목만 체크 (카드, 대출 등)
   POST /api/v1/mydata/filter
   { "persona": "OFFICE_WORKER", "selectedFields": ["cards", "loans"] }
   → 선택된 항목만 반환

③ 위기 유형 + 상황 설명 + 필터된 데이터로 분석 요청
   POST /api/v1/analysis/recommend
   {
     "crisisType": "UNEMPLOYMENT",
     "situationDescription": "권고사직 통보를 받았습니다.",
     "filteredMyData": { /* ②의 결과 */ }
   }
   → 할 일 · 받을 돈 · 미룰 것 · 행동 반환
```

---

### 3-4. LLM 폴백 체인

```
LlmProviderRouter.complete(request)
        │
        ├─▶ GeminiProvider.complete()
        │       성공 → 반환
        │       실패 (API 키 없음 / 네트워크 오류 / 타임아웃)
        │           └─▶ ClaudeProvider.complete()
        │                   성공 → 반환
        │                   실패
        │                       └─▶ OpenAiProvider.complete()
        │                               성공 → 반환
        │                               실패
        │                                   └─▶ CrisfinException(LLM_ALL_PROVIDERS_FAILED)
        │                                       HTTP 503 반환
        │
        ※ 우선순위: application.yml의 llm.providers.order 값으로 변경 가능
           기본값: GEMINI,CLAUDE,OPENAI
```

---

### 3-5. 복지 DB 배치 동기화

```
매일 03:00 (cron: "0 0 3 * * *")
        │
        ├─ API 키 설정 여부 확인
        │       미설정 → 스킵 (로컬/CI 환경 안전)
        │
        ├─ WelfareApiClient.getWelfareBenefits(page=1, perPage=100)
        │       공공 데이터 포털 (api.odcloud.kr) 호출
        │
        ├─ 각 항목별 upsert
        │       externalServiceId 존재 → update()
        │       신규 → insert
        │
        └─ saveAll() — 트랜잭션 커밋
               예외 발생 시 로그만 남기고 스케줄러 스레드 유지
```

---

## 4. 보안 레이어

```
요청 진입
   │
   ├─① CORS 필터
   │     허용 오리진: CORS_ALLOWED_ORIGINS (기본: localhost:3000, 5173)
   │
   ├─② JwtAuthenticationFilter
   │     Authorization: Bearer <token> 헤더 파싱
   │     유효 → SecurityContext에 인증 정보 저장
   │     없거나 만료 → 익명 처리 (permitAll 경로는 통과)
   │
   ├─③ PromptInjectionDetector (Analysis 요청 시)
   │     "ignore previous instructions", "system prompt" 등 패턴 감지
   │     탐지 시 → 400 PROMPT_INJECTION_DETECTED
   │
   ├─④ PiiMaskingService (Analysis 요청 시)
   │     계좌번호 → ***-****-****
   │     카드번호 → 앞 6자리만 유지
   │     주민번호 → 제거
   │     실명/고용주명 → 익명화
   │
   └─⑤ LlmResponseValidator (Analysis 응답 수신 시)
         JSON 파싱 검증 → 실패 시 최대 2회 재시도
         estimatedMax 1억 초과 → 경고 플래그
```

---

## 5. 데이터베이스 구조 요약

| 테이블 | 주요 컬럼 | 비고 |
|--------|-----------|------|
| `users` | id, email, password_hash, nickname, persona_type, role, is_active | BCrypt 해시 |
| `refresh_tokens` | id, user_id, token, expires_at | Rotation으로 로그인당 1개 유지 |
| `crisis_guides` | id, crisis_type (unique), title, coaching_prompt, key_rules (jsonb), source_laws (jsonb) | V2 시드 5건 |
| `mock_mydata_profiles` | id, persona (unique), financial_data (jsonb) | V3 시드 5개 페르소나 |
| `welfare_benefits` | id, external_service_id (unique), service_name, crisis_tags (jsonb), is_active, last_synced_at | V4 시드 10건 + 배치 갱신 |
| `analysis_results` | id, user_id (nullable), crisis_type, situation_description, input_mydata_json (jsonb), result_json (jsonb), llm_provider, tokens_used | 비로그인 허용 |

---

*최종 업데이트: 2026-06-28*

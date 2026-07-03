# DEPLOY.md — CrisFin 무료 배포 가이드 (시연용)

**구성**: 프론트 = **Vercel** · 백엔드(Docker) = **Render** · DB = **Neon**(Postgres 16 + pgvector)

```
[브라우저] ──same-origin──▶ Vercel(Next.js) ──/api/* 프록시(BACKEND_URL)──▶ Render(Spring Boot) ──▶ Neon(PG+pgvector)
```

프론트는 `next.config.ts` rewrites로 `/api/*`를 `BACKEND_URL`(Render)로 프록시한다. 브라우저는 same-origin이라 CORS 부담이 없다(단, 백엔드 CORS에 Vercel 도메인은 넣어 둔다).

> 배포 순서는 **① DB → ② 백엔드 → ③ 프론트** 다. 백엔드는 DB 연결정보가, 프론트는 백엔드 URL이 필요하기 때문.

---

## 사전 준비
- GitHub 저장소: `5-FIN/crisfin` (main 브랜치 최신 상태로 push)
- 계정: [Neon](https://neon.tech) · [Render](https://render.com) · [Vercel](https://vercel.com) — 모두 **GitHub 로그인**으로 무료 가입 가능
- OpenAI API 키 (실제 LLM 분석용) — 없으면 RAG/유료 분석만 자동 비활성화되고 나머지는 정상 동작

---

## ① Neon — PostgreSQL + pgvector

1. Neon 콘솔 → **New Project** (Region: `Asia Pacific (Singapore)` 권장, Postgres 16).
2. 생성되면 **Connection string**을 복사한다. 형태:
   ```
   postgresql://<user>:<password>@<host>/<dbname>?sslmode=require
   ```
3. 이 값을 백엔드가 쓰는 **JDBC 형식**으로 분해한다:
   | 백엔드 env | 값 |
   |-----------|----|
   | `DB_URL` | `jdbc:postgresql://<host>/<dbname>?sslmode=require` |
   | `DB_USERNAME` | `<user>` |
   | `DB_PASSWORD` | `<password>` |
   > `jdbc:` 접두사를 붙이고, user/password는 URL에서 빼서 별도 변수로 넣는 점에 주의.
4. pgvector 확장은 **Flyway 마이그레이션(V5/V9)** 이 앱 첫 부팅 시 `CREATE EXTENSION IF NOT EXISTS vector`로 자동 활성화한다. Neon은 pgvector를 지원하므로 별도 조작 불필요.

---

## ② Render — 백엔드 (Docker, Blueprint)

저장소 루트의 **`render.yaml`** 을 그대로 사용한다.

1. Render 대시보드 → **New → Blueprint** → `5-FIN/crisfin` 저장소 선택 → `render.yaml` 자동 인식.
2. `sync:false`로 표시된 환경변수를 입력:
   | env | 값 |
   |-----|----|
   | `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | ①의 Neon 값 |
   | `OPENAI_API_KEY` | 실제 OpenAI 키 |
   | `CORS_ALLOWED_ORIGINS` | (③ 후 확정) 우선 `https://crisfin.vercel.app` 예상값 |
   - `JWT_SECRET`은 Render가 자동 생성, `LLM_PROVIDERS_ORDER=OPENAI` 등 나머지는 blueprint 기본값.
3. **Apply** → 첫 빌드(멀티스테이지 Docker: gradle 빌드 → JRE 런타임, 수 분 소요).
4. 배포되면 백엔드 URL 확보: `https://crisfin-backend.onrender.com` (이름에 따라 다름).
5. **헬스체크**: `https://<backend>/v3/api-docs` 가 JSON 200을 반환하면 정상. `GET /api/v1/crisis/types` 로 DB 연결까지 확인.

> **무료 티어 주의**: 15분 무요청 시 슬립 → 다음 요청에서 콜드스타트(30초~1분). 시연 직전 한 번 깨워두면 매끄럽다.
> **부팅 최적화**: blueprint에서 `WELFARE_SYNC_ON_STARTUP=false`, `RAG_AUTO_INDEX_ON_STARTUP=false`로 두어 첫 부팅을 단축한다. RAG 근거가 필요하면 관리자 `POST /api/v1/admin/rag/reindex`로 나중에 색인.

---

## ③ Vercel — 프론트 (Next.js)

1. Vercel → **Add New → Project** → `5-FIN/crisfin` import.
2. **Root Directory**를 `frontend`로 지정(모노레포). Framework는 Next.js 자동 감지.
3. 환경변수 추가:
   | env | 값 |
   |-----|----|
   | `BACKEND_URL` | ②의 Render 백엔드 URL (예: `https://crisfin-backend.onrender.com`) |
4. **Deploy** → 프론트 URL 확보(예: `https://crisfin.vercel.app`).
5. **CORS 마무리**: 확정된 Vercel 도메인을 Render의 `CORS_ALLOWED_ORIGINS`에 반영 → 백엔드 재배포.

---

## ④ 스모크 테스트 (시연 전 확인)

1. `https://<vercel>` 접속 → 랜딩/위기 선택 렌더 확인.
2. 회원가입 → 로그인 (JWT 발급).
3. 무료 길라잡이(`/guide`) — 로그인 없이 조회되는지.
4. AI 분석(`/analysis/recommend`) — 위기 선택 → 마이데이터 → 4분면 결과 생성(실제 OpenAI 호출).
5. 복지 목록(`/welfare`) — 지역/위기 필터.

빠른 API 확인:
```bash
BACKEND=https://crisfin-backend.onrender.com
curl -s $BACKEND/api/v1/crisis/types | head
curl -s $BACKEND/v3/api-docs | head -c 200
```

---

## 환경변수 요약

| Key | 위치 | 설명 |
|-----|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Render | Neon 접속 (JDBC 형식) |
| `JWT_SECRET` | Render | 자동 생성(256bit+) |
| `OPENAI_API_KEY` | Render | 임베딩(RAG) + LLM 분석 |
| `LLM_PROVIDERS_ORDER` | Render | `OPENAI` (단독 사용) |
| `CORS_ALLOWED_ORIGINS` | Render | Vercel 도메인 |
| `WELFARE_SYNC_ON_STARTUP` | Render | `false`(무료 인스턴스 권장) |
| `RAG_AUTO_INDEX_ON_STARTUP` | Render | `false`(콜드스타트/비용↓) |
| `PORT` | Render | 자동 주입 → 앱이 `${PORT}` 바인딩 |
| `BACKEND_URL` | Vercel | 프록시 대상(Render 백엔드 URL) |

## 트러블슈팅
- **백엔드 500/부팅 실패**: Render 로그에서 Flyway/DB 연결 확인. `DB_URL`에 `?sslmode=require` 누락이 흔한 원인.
- **프론트 401/네트워크 오류**: `BACKEND_URL` 오타 또는 백엔드 슬립. 백엔드를 먼저 깨울 것.
- **CORS 차단**: `CORS_ALLOWED_ORIGINS`에 실제 Vercel 도메인(https 포함) 정확히 입력 후 백엔드 재배포.
- **AI 분석 503**: OpenAI 키 오류/한도. 키 확인 또는 `local-mock` 프로파일로 대체 시연.

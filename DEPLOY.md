# 무료 배포 가이드 (Vercel + Render + Neon)

CrisFin은 3개 요소로 나뉘며, 무료 티어로 전부 배포할 수 있습니다.

| 요소 | 서비스 | 티어 |
|---|---|---|
| 프론트 (Next.js, `frontend/`) | **Vercel** | Hobby (무료) |
| 백엔드 (Spring Boot, `backend/`) | **Render** (Docker 웹서비스) | Free |
| DB (Postgres + pgvector) | **Neon** | Free |

프론트는 `next.config.ts`의 rewrites로 `/api/*`를 `BACKEND_URL`(백엔드 공개 URL)로
서버사이드 프록시한다. 따라서 **백엔드를 먼저 배포**해 URL을 확보한 뒤 Vercel에 넣어야 한다.

---

## 1) DB — Neon (Postgres + pgvector)

1. https://neon.tech 가입 → New Project (region 아무거나).
2. 생성된 connection string 확인 (예: `postgresql://user:pass@ep-xxx.neon.tech/neondb?sslmode=require`).
3. pgvector는 앱의 Flyway 마이그레이션(V9)이 `CREATE EXTENSION vector`를 실행하므로 별도 작업 불필요.
   (Neon은 pgvector 지원)
4. 백엔드에 넣을 JDBC URL 형태로 변환:
   `jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require`

## 2) 백엔드 — Render (Docker)

1. https://render.com 가입 → New → **Web Service** → 이 GitHub 레포 연결.
2. 설정:
   - **Root Directory**: `backend`
   - **Runtime**: Docker (레포의 `backend/Dockerfile` 자동 사용)
   - **Instance Type**: Free
3. **Environment Variables**:
   | Key | Value |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require` |
   | `DB_USERNAME` | Neon user |
   | `DB_PASSWORD` | Neon password |
   | `JWT_SECRET` | 256bit 이상 랜덤 문자열 |
   | `OPENAI_API_KEY` | OpenAI 키 (또는 다른 LLM 키) |
   | `LLM_PROVIDERS_ORDER` | `OPENAI` (키 1개만 쓸 때 폴백 실패 방지) |
   | `CORS_ALLOWED_ORIGINS` | Vercel 프론트 도메인 (예: `https://crisfin.vercel.app`) |
   | `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0` (512MB 무료 인스턴스 OOM 방지, 권장) |
   | `WELFARE_SYNC_ON_STARTUP` | `false` (권장 — 아래 ⚠️ 참고) |
4. 배포 후 공개 URL 확인 (예: `https://crisfin-api.onrender.com`).
   - 앱은 `server.port: ${PORT:8080}`으로 Render가 주입하는 `$PORT`에 바인딩된다.
   - ⚠️ Free 인스턴스는 15분 미사용 시 잠들고 첫 요청에 ~50초 콜드스타트가 있다(데모엔 무방).
   - ⚠️ **복지 자동 동기화**: 기본값(`welfare.sync.on-startup=true`)은 첫 부팅 시 복지 1000건 +
     상세 + RAG 임베딩을 백그라운드로 돌려 512MB 무료 인스턴스엔 부담이 크고, 인스턴스가
     잠들면 부분 데이터로 멈출 수 있다. 그래서 `WELFARE_SYNC_ON_STARTUP=false`로 끄고,
     배포 후 관리자(`ROLE_ADMIN`)로 `POST /api/v1/admin/welfare/sync`를 한 번 호출하거나
     매일 03:00 스케줄러에 맡겨 채우는 것을 권장한다. (Neon은 영속이라 최초 1회면 충분)

## 3) 프론트 — Vercel

1. https://vercel.com → New Project → 이 GitHub 레포 import.
2. ⚠️ **Root Directory**: `frontend` (모노레포이므로 반드시 지정).
3. Framework: Next.js 자동 감지 / Production Branch: `main`.
4. **Environment Variables**:
   | Key | Value |
   |---|---|
   | `BACKEND_URL` | 2)에서 확보한 Render URL (예: `https://crisfin-api.onrender.com`) |
5. Deploy → 배포된 Vercel 도메인을 2)의 `CORS_ALLOWED_ORIGINS`에도 반영(재배포).

## 배포 순서 요약

Neon(DB) → Render(백엔드, URL 확보) → Vercel(프론트, BACKEND_URL 주입) →
Vercel 도메인을 Render의 CORS에 반영.

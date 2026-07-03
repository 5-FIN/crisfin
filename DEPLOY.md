# DEPLOY.md — CrisFin AWS EC2 배포 가이드 (시연용)

EC2 **한 대**에 `docker compose`로 전체 스택을 올리는 자체 호스팅 방식.

```
[브라우저] ──http──▶ EC2  :80 ─▶ frontend(Next.js :3000)
                              │  /api/* 서버사이드 프록시(BACKEND_URL)
                              ▼
                         backend(Spring Boot :8080) ─▶ postgres(pgvector :5432)
                                   └▶ OpenAI API
```

- `frontend`만 외부(:80)로 노출. `backend`·`postgres`는 docker 내부 네트워크로만 통신 → 공격면 최소화.
- 프론트가 `/api/*`를 컨테이너 네트워크의 `backend:8080`으로 프록시하므로 브라우저는 same-origin, CORS 부담 없음.
- pgvector 확장은 Flyway 마이그레이션(V5/V9)이 첫 부팅 시 `CREATE EXTENSION IF NOT EXISTS vector`로 자동 활성화.

> PaaS(무료 티어) 방식이 필요하면 루트 `render.yaml`(Render Blueprint)로 백엔드를 대신 올릴 수도 있다. 이 문서는 EC2 단일 인스턴스 기준.

---

## 0. 사전 준비
- AWS 계정 + EC2 접근 권한
- SSH 키페어(`.pem`)
- OpenAI API 키 (실제 LLM 분석용) — 없으면 RAG/유료 분석만 자동 비활성화, 나머지는 정상 동작
- 도메인은 선택(없으면 `http://<EC2-Public-IP>`로 시연)

---

## 1. EC2 인스턴스 생성

| 항목 | 권장값 | 비고 |
|---|---|---|
| AMI | Ubuntu Server 22.04 LTS (x86_64) | |
| 인스턴스 타입 | **t3.small (2 vCPU / 2GB)** | gradle+npm 동시 빌드 때문에 t2.micro(1GB)는 OOM 위험. 미프리티어면 swap 필수(3단계) |
| 스토리지 | 20GB gp3 | Docker 이미지+빌드 캐시 여유 |
| 키페어 | 기존/신규 `.pem` | SSH 접속용 |

**보안 그룹(인바운드)**
| 포트 | 소스 | 용도 |
|---|---|---|
| 22 (SSH) | 내 IP | 서버 접속 |
| 80 (HTTP) | 0.0.0.0/0 | 서비스 공개 |
| 443 (HTTPS) | 0.0.0.0/0 | (선택) TLS 적용 시 |

> `8080`·`5432`는 **열지 않는다**(내부 통신 전용). 노출 시 DB/백엔드가 직접 공격 대상이 됨.

접속:
```bash
ssh -i <키>.pem ubuntu@<EC2-Public-IP>
```

---

## 2. Docker 설치 (EC2에서)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu && newgrp docker   # sudo 없이 docker 사용
docker --version && docker compose version
```

---

## 3. (t2.micro 등 저메모리) 스왑 추가 — 권장

빌드 중 OOM 방지. 2GB 이상 인스턴스도 안전 마진으로 권장.
```bash
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h   # Swap 확인
```

---

## 4. 소스 클론 + 환경변수

```bash
git clone https://github.com/5-FIN/crisfin.git
cd crisfin
```

배포용 `.env` 작성 (루트, docker compose가 자동 로드):
```bash
cat > .env <<'EOF'
# DB 비밀번호(원하는 값으로)
DB_PASSWORD=change-me-strong-pw
# JWT 서명키 — 256bit(32바이트) 이상. 아래로 생성 가능:  openssl rand -base64 48
JWT_SECRET=<openssl rand -base64 48 결과 붙여넣기>
# 실제 OpenAI 키
OPENAI_API_KEY=sk-...
# 공개 오리진 (도메인 없으면 http://<EC2-Public-IP>)
PUBLIC_ORIGIN=http://<EC2-Public-IP>
EOF
```

---

## 5. 배포용 compose 작성 (`docker-compose.prod.yml`)

루트에서 아래 파일을 만든다. (레포의 기본 `docker-compose.yml`은 로컬 DB 전용이라 전체 스택은 이 파일로 구동)
```bash
cat > docker-compose.prod.yml <<'EOF'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: crisfin
      POSTGRES_USER: crisfin
      POSTGRES_PASSWORD: ${DB_PASSWORD:-crisfin}
    volumes:
      - crisfin-pg-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U crisfin -d crisfin"]
      interval: 5s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/crisfin
      DB_USERNAME: crisfin
      DB_PASSWORD: ${DB_PASSWORD:-crisfin}
      JWT_SECRET: ${JWT_SECRET}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      LLM_PROVIDERS_ORDER: OPENAI
      CORS_ALLOWED_ORIGINS: ${PUBLIC_ORIGIN}
      # 무료/저사양 인스턴스 부팅 최적화
      WELFARE_SYNC_ON_STARTUP: "false"
      RAG_AUTO_INDEX_ON_STARTUP: "false"
      # 컨테이너 메모리 한도 기준으로 JVM 힙 산정 (OOM 방지)
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75.0"
    expose:
      - "8080"          # 내부 네트워크만. 호스트로 노출하지 않음.
    restart: unless-stopped

  frontend:
    image: node:20-alpine
    working_dir: /app
    depends_on:
      - backend
    environment:
      BACKEND_URL: http://backend:8080   # 컨테이너 네트워크 프록시 대상
    volumes:
      - ./frontend:/app
    command: sh -c "npm ci && npm run build && npm start"
    ports:
      - "80:3000"       # 외부 공개
    restart: unless-stopped

volumes:
  crisfin-pg-data:
EOF
```

---

## 6. 빌드 & 기동

```bash
docker compose -f docker-compose.prod.yml up -d --build
```
- 최초 실행은 백엔드 gradle 빌드 + 프론트 npm 빌드로 **수 분** 소요.
- 진행/로그 확인:
```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend
```

> 저메모리에서 동시 빌드가 버거우면 순차 기동: `... up -d --build postgres backend` 로 백엔드부터 올린 뒤 `... up -d --build frontend`.

---

## 7. 스모크 테스트

브라우저에서 `http://<EC2-Public-IP>` 접속 후:
1. 랜딩/위기 선택 렌더 확인
2. 회원가입 → 로그인 (JWT 발급)
3. 무료 길라잡이(`/guide`) — 비로그인 조회
4. AI 분석 — 위기 선택 → 마이데이터 → 4분면 결과(실제 OpenAI 호출)
5. 복지 목록(`/welfare`) — 지역/위기 필터

서버 내부 확인(컨테이너 네트워크):
```bash
docker compose -f docker-compose.prod.yml exec frontend wget -qO- http://backend:8080/v3/api-docs | head -c 200
docker compose -f docker-compose.prod.yml exec frontend wget -qO- http://backend:8080/api/v1/crisis/types | head
```

---

## 8. 환경변수 요약

| Key | 위치 | 설명 |
|---|---|---|
| `DB_PASSWORD` | `.env` | Postgres 비밀번호 |
| `JWT_SECRET` | `.env` | JWT 서명키(256bit+, `openssl rand -base64 48`) |
| `OPENAI_API_KEY` | `.env` | 임베딩(RAG) + LLM 분석 |
| `PUBLIC_ORIGIN` | `.env` | 공개 오리진(도메인 또는 `http://<EC2-IP>`) → 백엔드 CORS |
| `LLM_PROVIDERS_ORDER` | compose | `OPENAI` (단독) |
| `WELFARE_SYNC_ON_STARTUP` | compose | `false`(부팅 단축) |
| `RAG_AUTO_INDEX_ON_STARTUP` | compose | `false`(콜드스타트/비용↓) |
| `JAVA_TOOL_OPTIONS` | compose | `-XX:MaxRAMPercentage=75.0`(OOM 방지) |
| `BACKEND_URL` | compose(frontend) | `http://backend:8080`(내부 프록시) |

---

## 9. 운영 메모

- **업데이트 배포**: `git pull && docker compose -f docker-compose.prod.yml up -d --build`
- **데이터 영속**: `crisfin-pg-data` 볼륨에 저장 → 컨테이너 재생성해도 유지. 완전 초기화: `docker compose -f docker-compose.prod.yml down -v`
- **복지 데이터 채우기**: 부팅 자동동기화를 껐으므로, 필요 시 관리자(`ROLE_ADMIN`)로 `POST /api/v1/admin/welfare/sync` 1회 호출(공공데이터 키 필요) 또는 매일 03:00 스케줄러에 위임.
- **RAG 근거**: 필요 시 `POST /api/v1/admin/rag/reindex`로 임베딩 색인.
- **HTTPS(선택)**: 도메인이 있으면 프론트 앞단에 Caddy/nginx 리버스 프록시로 Let's Encrypt 자동 TLS. 데모는 http로 충분.
- **비용 절감**: 시연 후 `docker compose ... down`하거나 EC2 **중지**(Stop). 볼륨/EBS는 유지되므로 재기동 시 데이터 보존.

## 10. 트러블슈팅
- **backend 기동 실패/OOM**: `logs backend`에서 Flyway/DB 연결 확인. 메모리 부족이면 스왑(3단계) 추가 또는 인스턴스 상향. `free -h`로 메모리 확인.
- **frontend 502/접속 불가**: `logs frontend`로 빌드 완료 여부 확인(`npm run build` 시간 소요). 보안 그룹 80 포트 오픈 확인.
- **DB 연결 오류**: `postgres` 헬스체크 통과 후 backend가 뜨는지 확인(`depends_on: condition: service_healthy`).
- **AI 분석 503**: OpenAI 키 오류/한도. 키 확인 또는 백엔드 env를 `SPRING_PROFILES_ACTIVE=local-mock`로 두고 mock 응답으로 시연.

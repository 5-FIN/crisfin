# CrisFin — 위기 상황 금융 길라잡이

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)

---

## 프로젝트 소개

대한민국에는 **11조 2천억 원**에 달하는 미청구 보험금이 잠들어 있습니다. 갑작스러운 실직, 질병, 사고 등 위기 상황이 닥쳤을 때 사람들은 본인이 받을 수 있는 보험금과 정부 지원금이 무엇인지 파악하지 못한 채 경제적 어려움을 홀로 감당합니다. CrisFin은 위기 유형을 선택하면 마이데이터 기반으로 보유 금융 정보를 분석하고, AI가 청구 가능한 보험금·정부 혜택·금융 상품을 한눈에 정리해 주는 서비스입니다. 복잡한 금융 절차를 몰라 놓치는 권리 없이, 누구나 위기를 슬기롭게 극복할 수 있도록 돕습니다.

---

## 서비스 작동 방식

```
1. 위기 선택         2. 마이데이터          3. AI 분석            4. 4탭 결과
─────────────    ──────────────────    ──────────────    ──────────────────────
실직 / 질병 /    마이데이터 연동으로    보유 보험·자산·    보험금 청구 가이드
사고 / 재난 중   보유 보험·금융 정보    지원 자격을        정부 지원금 안내
하나를 선택      자동 수집              AI가 종합 분석     대출 상품 추천
                                                          맞춤 액션플랜 제시
```

| 단계 | 내용 |
|------|------|
| **1. 위기 선택** | 현재 처한 위기 유형(실직, 질병, 사고, 재난 등)을 선택합니다. |
| **2. 마이데이터 연동** | 동의 기반 마이데이터 연동으로 보유 보험·금융 정보를 자동 수집합니다. |
| **3. AI 분석** | AI가 위기 유형과 금융 데이터를 종합 분석하여 청구 가능성을 판단합니다. |
| **4. 4탭 결과 제공** | 보험금 청구 / 정부 지원금 / 금융 상품 / 액션플랜을 탭별로 안내합니다. |

---

## 로컬 실행 방법

### Prerequisites

- Java 21 이상
- Gradle 8.x
- PostgreSQL 16 이상
- (선택) Docker & Docker Compose

### DB 설정

```sql
-- PostgreSQL에서 데이터베이스 및 사용자 생성
CREATE DATABASE crisfin;
CREATE USER crisfin_user WITH PASSWORD 'crisfin_password';
GRANT ALL PRIVILEGES ON DATABASE crisfin TO crisfin_user;
```

`src/main/resources/application.yml` 또는 환경변수로 DB 접속 정보를 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crisfin
    username: crisfin_user
    password: crisfin_password
```

### 실행

```bash
# 프로젝트 루트에서
./gradlew bootRun
```

서버가 정상 기동되면 `http://localhost:8080` 에서 확인할 수 있습니다.

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/v1/auth/register` | 불필요 | 회원가입 |
| `POST` | `/api/v1/auth/login` | 불필요 | 로그인 |
| `POST` | `/api/v1/auth/logout` | 필요 | 로그아웃 |
| `GET` | `/api/v1/users/me` | 필요 | 내 프로필 조회 |
| `PUT` | `/api/v1/users/me` | 필요 | 내 프로필 수정 |
| `POST` | `/api/v1/crisis` | 필요 | 위기 분석 요청 |
| `GET` | `/api/v1/crisis/{crisisId}` | 필요 | 분석 결과 조회 |
| `POST` | `/api/v1/mydata/connect` | 필요 | 마이데이터 연동 |
| `GET` | `/api/v1/mydata/status` | 필요 | 연동 상태 조회 |
| `GET` | `/api/v1/insurances` | 필요 | 보험 목록 조회 |
| `GET` | `/api/v1/insurances/{id}/claim` | 필요 | 청구 가이드 조회 |
| `GET` | `/api/v1/benefits` | 필요 | 지원금·혜택 조회 |

전체 상세 명세: [docs/API.md](docs/API.md)

---

## 팀 소개

**5FIN** — 5명이 만드는 금융 서비스

| 역할 | 담당 |
|------|------|
| PM / Backend | 팀원 1 |
| Backend | 팀원 2 |
| Backend | 팀원 3 |
| Frontend | 팀원 4 |
| Frontend | 팀원 5 |

---

## 기술 스택

### Backend

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Build Tool | Gradle |
| API Docs | Swagger (springdoc-openapi) |

### Database

| 분류 | 기술 |
|------|------|
| RDBMS | PostgreSQL 16 |
| Migration | Flyway |
| Cache | Redis (세션 및 토큰 관리) |

### Infrastructure

| 분류 | 기술 |
|------|------|
| Container | Docker / Docker Compose |
| CI/CD | GitHub Actions |
| Cloud | AWS EC2 / RDS |

### Conventions

- 브랜치 전략 및 커밋 컨벤션: [docs/CONVENTION.md](docs/CONVENTION.md)
- API 명세: [docs/API.md](docs/API.md)

# CrisFin API 명세

## 기본 정보

| 항목 | 값 |
|------|----|
| Base URL | `http://localhost:8080` |
| API Prefix | `/api/v1` |
| 콘텐츠 타입 | `application/json` |
| 인증 방식 | JWT Bearer Token |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |

---

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { },
  "message": null
}
```

### 실패 응답

```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

### HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| `200 OK` | 요청 성공 |
| `201 Created` | 리소스 생성 성공 |
| `400 Bad Request` | 요청 파라미터 오류 |
| `401 Unauthorized` | 인증 토큰 없음 또는 만료 |
| `403 Forbidden` | 권한 없음 |
| `404 Not Found` | 리소스 없음 |
| `500 Internal Server Error` | 서버 내부 오류 |

---

## 인증 (Auth)

JWT Bearer Token을 사용합니다. 인증이 필요한 엔드포인트에는 요청 헤더에 토큰을 포함합니다.

```
Authorization: Bearer {accessToken}
```

---

## 엔드포인트 목록

| # | Method | URL | 인증 여부 | 설명 |
|---|--------|-----|-----------|------|
| 1 | `POST` | `/api/v1/auth/register` | 불필요 | 회원가입 |
| 2 | `POST` | `/api/v1/auth/login` | 불필요 | 로그인 및 JWT 발급 |
| 3 | `POST` | `/api/v1/auth/logout` | 필요 | 로그아웃 (토큰 무효화) |
| 4 | `GET` | `/api/v1/users/me` | 필요 | 내 프로필 조회 |
| 5 | `PUT` | `/api/v1/users/me` | 필요 | 내 프로필 수정 |
| 6 | `POST` | `/api/v1/crisis` | 필요 | 위기 상황 선택 및 분석 요청 |
| 7 | `GET` | `/api/v1/crisis/{crisisId}` | 필요 | 위기 분석 결과 조회 |
| 8 | `POST` | `/api/v1/mydata/connect` | 필요 | 마이데이터 연동 요청 |
| 9 | `GET` | `/api/v1/mydata/status` | 필요 | 마이데이터 연동 상태 조회 |
| 10 | `GET` | `/api/v1/insurances` | 필요 | 보유 보험 목록 조회 |
| 11 | `GET` | `/api/v1/insurances/{insuranceId}/claim` | 필요 | 보험금 청구 가이드 조회 |
| 12 | `GET` | `/api/v1/benefits` | 필요 | 정부 지원금 및 혜택 목록 조회 |

---

## 엔드포인트 상세

### 1. 회원가입

**POST** `/api/v1/auth/register`

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동"
}
```

**Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  },
  "message": null
}
```

---

### 2. 로그인

**POST** `/api/v1/auth/login`

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "message": null
}
```

---

### 3. 로그아웃

**POST** `/api/v1/auth/logout`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": null,
  "message": null
}
```

---

### 4. 내 프로필 조회

**GET** `/api/v1/users/me`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "createdAt": "2025-01-01T00:00:00"
  },
  "message": null
}
```

---

### 5. 내 프로필 수정

**PUT** `/api/v1/users/me`

**Headers** `Authorization: Bearer {accessToken}`

**Request Body**

```json
{
  "name": "김철수"
}
```

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "김철수"
  },
  "message": null
}
```

---

### 6. 위기 상황 선택 및 분석 요청

**POST** `/api/v1/crisis`

**Headers** `Authorization: Bearer {accessToken}`

**Request Body**

```json
{
  "crisisType": "JOB_LOSS",
  "description": "갑작스러운 권고사직으로 실직 상태입니다."
}
```

**Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "crisisId": 42,
    "crisisType": "JOB_LOSS",
    "status": "ANALYZING"
  },
  "message": null
}
```

---

### 7. 위기 분석 결과 조회

**GET** `/api/v1/crisis/{crisisId}`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "crisisId": 42,
    "crisisType": "JOB_LOSS",
    "status": "COMPLETED",
    "insuranceSummary": { },
    "benefitSummary": { },
    "loanSummary": { },
    "actionPlanSummary": { }
  },
  "message": null
}
```

---

### 8. 마이데이터 연동 요청

**POST** `/api/v1/mydata/connect`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "consentUrl": "https://mydata.example.com/consent?token=abc123"
  },
  "message": null
}
```

---

### 9. 마이데이터 연동 상태 조회

**GET** `/api/v1/mydata/status`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "connected": true,
    "connectedAt": "2025-06-01T09:00:00",
    "lastSyncedAt": "2025-06-28T12:00:00"
  },
  "message": null
}
```

---

### 10. 보유 보험 목록 조회

**GET** `/api/v1/insurances`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "insuranceId": 1,
      "insuranceName": "실손의료보험",
      "insurer": "삼성생명",
      "coverageAmount": 50000000,
      "claimable": true
    }
  ],
  "message": null
}
```

---

### 11. 보험금 청구 가이드 조회

**GET** `/api/v1/insurances/{insuranceId}/claim`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "insuranceId": 1,
    "insuranceName": "실손의료보험",
    "claimSteps": [
      "1. 병원 진단서 및 영수증 준비",
      "2. 보험사 고객센터 또는 앱에서 청구 신청",
      "3. 서류 제출 후 영업일 5일 내 처리"
    ],
    "requiredDocuments": ["진단서", "진료비 영수증", "통장 사본"],
    "contactInfo": "1588-0000"
  },
  "message": null
}
```

---

### 12. 정부 지원금 및 혜택 목록 조회

**GET** `/api/v1/benefits`

**Headers** `Authorization: Bearer {accessToken}`

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "benefitId": 1,
      "benefitName": "실업급여",
      "category": "고용",
      "amount": "최대 월 198만원",
      "eligibility": "비자발적 실직 후 고용보험 가입 180일 이상",
      "applicationUrl": "https://www.ei.go.kr"
    }
  ],
  "message": null
}
```

# CrisFin 개발 컨벤션

## 브랜치 전략

| 브랜치 | 용도 | 비고 |
|--------|------|------|
| `main` | 배포용 최종 브랜치 | **직접 push 금지** |
| `develop` | 기능 통합 브랜치 | PR을 통해서만 병합 |
| `feature/[이슈번호]-[설명]` | 새 기능 개발 | 예: `feature/12-user-auth` |
| `fix/[이슈번호]-[설명]` | 버그 수정 | 예: `fix/34-login-error` |
| `hotfix/[이슈번호]-[설명]` | 운영 긴급 수정 | main에서 분기, main/develop 양쪽 병합 |

### 브랜치 운영 규칙

- `feature/*`, `fix/*` 브랜치는 항상 `develop`에서 분기합니다.
- 작업 완료 후 `develop`으로 PR을 올립니다.
- `main` 브랜치는 배포 담당자만 `develop` → `main` PR을 통해 병합합니다.
- 브랜치 이름의 설명 부분은 영문 소문자와 하이픈(`-`)만 사용합니다.

---

## 커밋 컨벤션 (Conventional Commits)

### 타입

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (코드 변경 없음) |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드 설정, 의존성 등 기타 변경 |

### 형식

```
타입: 설명 (#이슈번호)
```

**예시**

```
feat: 보험금 청구 분석 API 구현 (#15)
fix: JWT 만료 시 NullPointerException 수정 (#22)
docs: API 명세 엔드포인트 목록 업데이트 (#30)
refactor: InsuranceService 중복 로직 제거 (#18)
test: UserController 단위 테스트 추가 (#25)
chore: Gradle 의존성 버전 업그레이드 (#10)
```

### 규칙

- 설명은 명령형 동사로 시작합니다 (구현, 수정, 추가, 제거 등).
- 설명은 50자 이내로 작성합니다.
- 이슈 번호는 반드시 포함합니다.
- 본문이 필요한 경우 빈 줄로 구분 후 72자 단위로 작성합니다.

---

## PR 규칙

### 기본 원칙

- **최소 1인 Approve** 를 받아야 병합할 수 있습니다.
- PR 생성 후 **24시간 이내** 에 리뷰어는 응답해야 합니다.
- 병합 방식은 **Squash and Merge** 를 사용합니다.
- PR 제목은 커밋 컨벤션 형식을 따릅니다.

### 리뷰 라벨

| 라벨 | 의미 |
|------|------|
| `[질문]` | 코드 의도나 맥락이 궁금할 때 |
| `[제안]` | 더 나은 방법을 제안할 때 (merge 블로킹 아님) |
| `[필수수정]` | 반드시 수정 후 merge해야 할 사항 |
| `[칭찬]` | 잘 작성된 코드에 긍정적인 피드백 |

### PR 체크리스트

- [ ] 관련 이슈 번호가 연결되어 있는가
- [ ] 불필요한 주석 및 디버그 로그가 제거되었는가
- [ ] 테스트 코드가 포함되어 있는가 (신규 기능의 경우)
- [ ] `ApiResponse<T>` 래핑이 적용되어 있는가

---

## 코드 스타일

### 패키지 구조

```
com.finfive.crisfin.domain.{도메인}
├── controller
├── service
├── repository
├── dto
│   ├── request
│   └── response
└── entity
```

**예시**

```
com.finfive.crisfin.domain.insurance.controller
com.finfive.crisfin.domain.insurance.service
com.finfive.crisfin.domain.user.entity
```

### API URL 규칙

- 모든 엔드포인트는 `@RequestMapping("/api/v1/{도메인}")` 으로 시작합니다.
- URL은 복수형 명사를 사용합니다 (예: `/users`, `/insurances`).
- 경로 변수는 카멜케이스를 사용하지 않고 소문자 + 하이픈을 사용합니다.

```java
@RestController
@RequestMapping("/api/v1/insurances")
public class InsuranceController { ... }
```

### 응답 형식

모든 API 응답은 반드시 `ApiResponse<T>` 로 래핑합니다.

```java
// 성공 응답
return ResponseEntity.ok(ApiResponse.success(data));

// 실패 응답
return ResponseEntity.badRequest().body(ApiResponse.error("에러 메시지"));
```

### 예외 처리

커스텀 예외는 `CrisfinException` 을 상속하여 사용합니다.

```java
throw new CrisfinException(ErrorCode.INSURANCE_NOT_FOUND);
```

- 비즈니스 예외는 `ErrorCode` enum에 코드와 메시지를 정의합니다.
- `@RestControllerAdvice` 의 `GlobalExceptionHandler` 에서 일괄 처리합니다.
- `try-catch` 로 직접 HTTP 상태코드를 반환하지 않습니다.

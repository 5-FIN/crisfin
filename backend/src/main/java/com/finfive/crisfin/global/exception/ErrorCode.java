package com.finfive.crisfin.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 – Domain / Validation
    CRISIS_TYPE_NOT_FOUND(400, "CRISIS_TYPE_NOT_FOUND", "요청한 위기 유형을 찾을 수 없습니다."),
    GUIDE_NOT_FOUND(400, "GUIDE_NOT_FOUND", "요청한 가이드를 찾을 수 없습니다."),
    PERSONA_NOT_FOUND(400, "PERSONA_NOT_FOUND", "요청한 페르소나를 찾을 수 없습니다."),
    PII_DETECTED(400, "PII_DETECTED", "개인 식별 정보(PII)가 감지되었습니다. 입력을 수정해 주세요."),
    PROMPT_INJECTION_DETECTED(400, "PROMPT_INJECTION_DETECTED", "프롬프트 인젝션 시도가 감지되었습니다."),
    PLAN_NOT_FOUND(400, "PLAN_NOT_FOUND", "요청한 요금제를 찾을 수 없습니다."),

    // 401 – Authentication
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다."),
    TOKEN_INVALID(401, "TOKEN_INVALID", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),

    // 402 – Payment
    PAYMENT_REQUIRED(402, "PAYMENT_REQUIRED", "분석 이용권이 필요합니다. 결제 후 이용해 주세요."),

    // 403 – Authorization
    FORBIDDEN(403, "FORBIDDEN", "해당 리소스에 대한 접근 권한이 없습니다."),

    // 429 – Too many requests
    LOGIN_LOCKED(429, "LOGIN_LOCKED", "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // 404 – Not found
    ANALYSIS_NOT_FOUND(404, "ANALYSIS_NOT_FOUND", "요청한 분석 결과를 찾을 수 없습니다."),
    PAYMENT_ORDER_NOT_FOUND(404, "PAYMENT_ORDER_NOT_FOUND", "결제 주문을 찾을 수 없습니다."),
    WELFARE_NOT_FOUND(404, "WELFARE_NOT_FOUND", "요청한 복지 제도를 찾을 수 없습니다."),

    // 500 – Internal
    LLM_RESPONSE_PARSE_ERROR(500, "LLM_RESPONSE_PARSE_ERROR", "LLM 응답을 파싱하는 데 실패했습니다."),

    // 503 – External / Infra
    LLM_ALL_PROVIDERS_FAILED(503, "LLM_ALL_PROVIDERS_FAILED", "모든 LLM 프로바이더가 응답에 실패했습니다."),
    WELFARE_API_UNAVAILABLE(503, "WELFARE_API_UNAVAILABLE", "복지 서비스 API를 현재 사용할 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}

package com.finfive.crisfin.global.exception;

import lombok.Getter;

@Getter
public class CrisfinException extends RuntimeException {

    private final ErrorCode errorCode;

    public CrisfinException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CrisfinException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public CrisfinException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}

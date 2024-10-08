package com.studyNook.global.common.exception.code;

import com.studyNook.global.common.exception.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.apache.commons.lang3.StringUtils.leftPad;

@Getter
@RequiredArgsConstructor
public enum AuthResponseCode implements ResponseCode {

    // 공통 코드 정의
    UNAUTHORIZED("1", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("2", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("3", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_TOKEN_ERROR("4", HttpStatus.NOT_FOUND),
    FORBIDDEN("5", HttpStatus.FORBIDDEN),
    AUTH_FAIL("6", HttpStatus.UNAUTHORIZED),
    ;

    private final String code;
    private final HttpStatus httpStatus;

    public String getCode() {
        return String.format("I-AUT-%s", leftPad(code, 4, "0"));
    }
}

package com.studyNook.global.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Objects;
import java.util.stream.Collectors;


import static com.studyNook.global.common.exception.RestResponse.toResponseEntity;
import static com.studyNook.global.common.exception.code.AuthResponseCode.TOKEN_EXPIRED;
import static com.studyNook.global.common.exception.code.CommonResponseCode.*;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Message message;

    @ExceptionHandler(value = {NoHandlerFoundException.class})
    protected ResponseEntity<RestResponse> noHandlerFoundException(NoHandlerFoundException e) {
        var apiNotFound = API_NOT_FOUND;
        String responseMessage = "";
        try {
            responseMessage = message.getResponseMessage(apiNotFound, e.getMessage(), e.getMessage());
        } catch (Exception ex) {
            responseMessage = e.getMessage();
        }
        return status(apiNotFound.getHttpStatus()).body(RestResponse.builder()
                .code(apiNotFound.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    protected ResponseEntity<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @ExceptionHandler(value = {CustomException.class})
    protected ResponseEntity<RestResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException = {}", getStackTrace(e));
        var response = message.getResponseMessage(e.getResponseCode(), e.getMessage(), e.getValues());
        return toResponseEntity(e.getResponseCode(), response);
    }

    @ExceptionHandler(value = {WebClientRetrieveException.class})
    protected ResponseEntity<RestResponse> handleStackExchangeException(WebClientRetrieveException e) {
        log.error("handleStackExchangeException = {}", getStackTrace(e));
        return toResponseEntity(e);
    }

    @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
    protected ResponseEntity<RestResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        var code = PARAMETER_ERROR;
        String responseMessage;
        try {
            var typeCheckMessage = String.format("required type %s, invalid value=[%s]",
                    Objects.requireNonNull(e.getRequiredType()).getSimpleName(),
                    e.getValue());
            responseMessage = message.getResponseMessage(code, e.getMessage(), typeCheckMessage);
        } catch (Exception ex) {
            responseMessage = e.getMessage();
        }
        return status(code.getHttpStatus()).body(RestResponse.builder()
                .code(code.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {ServletRequestBindingException.class})
    protected ResponseEntity<RestResponse> handleServletRequestBindingException(ServletRequestBindingException e) {
        log.error("ServletRequestBindingException = {}", ExceptionUtils.getStackTrace(e));
        var badRequest = BAD_REQUEST_ERROR;
        String responseMessage;
        try {
            responseMessage = message.getResponseMessage(badRequest, e.getMessage(),  e.getMessage());
        } catch (Exception ex) {
            log.error("ServletRequestBindingException = {}", ExceptionUtils.getStackTrace(ex));
            responseMessage = e.getMessage();
        }
        return status(badRequest.getHttpStatus()).body(RestResponse.builder()
                .code(badRequest.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    protected ResponseEntity<RestResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        var code = PARAMETER_ERROR;
        var responseMessage = "";
        try {
            var shortMessage = substringBefore(e.getMessage(), ":");
            responseMessage = message.getResponseMessage(code, e.getMessage(),  shortMessage);
        } catch (Exception ex) {
            responseMessage = "입력값 포맷 또는 값이 잘못 되었습니다.";
        }
        return status(code.getHttpStatus()).body(RestResponse.builder()
                .code(code.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {HttpMediaTypeNotSupportedException.class})
    protected ResponseEntity<RestResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        var code = PARAMETER_ERROR;
        var responseMessage = "";
        try {
            responseMessage = message.getResponseMessage(code, e.getMessage(),  e.getMessage());
        } catch (Exception ex) {
            responseMessage = "입력값 포맷 또는 값이 잘못 되었습니다.";
        }
        return status(code.getHttpStatus()).body(RestResponse.builder()
                .code(code.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {RequiredParameterException.class})
    protected ResponseEntity<RestResponse> handleRequiredParameterException(RequiredParameterException e) {
        log.error("RequiredParameterException = {}", getStackTrace(e));

        var response = message.getResponseMessage(e.getResponseCode(), e.getMessage(), e.getMessage());
        return toResponseEntity(e.getResponseCode(), response);
    }

    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    protected ResponseEntity<RestResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        var code = REQUIRED_ERROR;
        String responseMessage;
        try {
            responseMessage = message.getResponseMessage(code, e.getMessage(), e.getParameterName());
        } catch (Exception ex) {
            responseMessage = e.getMessage();
        }
        return status(code.getHttpStatus()).body(RestResponse.builder()
                .code(code.getCode())
                .message(responseMessage)
                .build());
    }

    @ExceptionHandler(value = {BindException.class})
    protected ResponseEntity<RestResponse> springValidationException(BindException e) {
        String message = e.getAllErrors().stream()
                .map(error -> String.format("%s: %s", ((FieldError) error).getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(" / "));

        log.error("springValidationException = {}", message);
        return toResponseEntity(PARAMETER_ERROR, message);
    }

    @ExceptionHandler(value = ExpiredJwtException.class)
    protected ResponseEntity<RestResponse> expiredTokenException(ExpiredJwtException e) {
        log.error("만료된 JWT 토큰");
        var response = message.getResponseMessage(TOKEN_EXPIRED, e.getMessage());
        return toResponseEntity(TOKEN_EXPIRED, response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<RestResponse> constraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage());
        var customMessage = e.getConstraintViolations()
                .stream()
                .map(violation ->((PathImpl)violation.getPropertyPath()).getLeafNode().getName())
                .collect(Collectors.joining(","));
        var response = message.getResponseMessage(PARAMETER_ERROR, "", customMessage);
        return toResponseEntity(PARAMETER_ERROR, response);
    }

    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<RestResponse> handleException(Exception e) {
        log.error("exception = {}", getStackTrace(e));
        var response = message.getResponseMessage(UNKNOWN_ERROR, "Internal Server Error");
        return toResponseEntity(UNKNOWN_ERROR, response);
    }
}

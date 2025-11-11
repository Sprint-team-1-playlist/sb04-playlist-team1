package com.codeit.playlist.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("커스텀 예외 발생: code={}, message={}", e.getErrorCode(), e.getMessage(), e);
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, Object> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            Object rejectedValue = fieldError.getRejectedValue();
            details.put(fieldName, rejectedValue);
        });

        DomainException domainException = new DomainException(details);
        log.error("요청 유효성 커스텀 예외 발생: code={}, message={}", domainException.getErrorCode(), domainException.getMessage(), domainException);

        return ResponseEntity.status(domainException.getStatus())
                .body(new ErrorResponse(domainException));
    }

    // 필수 쿼리 파라미터 누락 → 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("param", e.getParameterName());
        details.put("expectedType", e.getParameterType());
        details.put("reason", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(new DomainException(details)));
    }
}

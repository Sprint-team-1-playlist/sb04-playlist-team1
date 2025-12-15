package com.codeit.playlist.global.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e);
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
            String simpleName = fieldName.contains(".")
                ? fieldName.substring(fieldName.lastIndexOf('.') + 1)
                : fieldName;

            // 민감 정보 필드 목록
            Set<String> sensitiveFields = Set.of(
                "password", "username", "email",
                "token", "accesstoken", "refreshtoken",
                "apikey", "secretkey", "secret"
            );

            if (sensitiveFields.contains(simpleName.toLowerCase())) {
                details.put(fieldName, "[REDACTED]");
            } else {
                // 그 외 필드는 rejectedValue 허용
                details.put(fieldName, fieldError.getRejectedValue());
            }
        });

        DomainException domainException =
            new DomainException(details);

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

    //타입 미스 매치 -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("name", e.getName());               // 파라미터/변수 이름
        details.put("value", e.getValue());             // 들어온 값
        details.put("requiredType",
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : null);
        DomainException domainException = new DomainException(details);

        return ResponseEntity.status(domainException.getStatus()).body(new ErrorResponse(domainException));

    }
}

package com.codeq.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 全局异常处理（contracts/api.md）：统一错误码 + traceId。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse build(int status, String error, String message) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, MDC.get("traceId"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> notFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(build(404, "Not Found", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> badRequest(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(400).body(build(400, "Bad Request", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(build(409, "Conflict", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internal(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return ResponseEntity.status(500).body(build(500, "Internal Server Error", e.getMessage()));
    }

    /** 统一错误响应（含 traceId，宪法 VIII）。 */
    public record ErrorResponse(String timestamp, int status, String error, String message, String traceId) {
    }
}

package com.itsm.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * One place that turns every error into the same JSON: {"error": "message"}.
 * The React app already reads err.response.data.error, so nothing changes there.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Our own exceptions (400 / 401 / 403 / 404 / 409)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getMessage()));
    }

    // Database rule broken (duplicate value, record still in use)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
            "Operation conflicts with existing data (duplicate value or record in use)."));
    }

    // Anything we did not expect: log the details, but never show them to the user
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Something went wrong on the server. Please try again."));
    }

    // @Valid failed on a request body
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining(" "));
        if (message.isBlank()) message = "Invalid request.";
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // Body is not valid JSON
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed or invalid request body."));
    }

    // Everything else Spring MVC throws (wrong URL, wrong method, bad parameter type...)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = switch (statusCode.value()) {
            case 400 -> "Bad request: a parameter is missing or has the wrong type.";
            case 404 -> "Resource not found.";
            case 405 -> "This HTTP method is not allowed for this URL.";
            case 415 -> "Unsupported content type.";
            default -> "Request could not be processed.";
        };
        return ResponseEntity.status(statusCode).headers(headers).body(Map.of("error", message));
    }
}
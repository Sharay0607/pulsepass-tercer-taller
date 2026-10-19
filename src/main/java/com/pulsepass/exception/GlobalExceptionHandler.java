package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Único punto de traducción de excepciones a HTTP (PRD §14 y §15).
 * Todos los handlers retornan {@code ResponseEntity<ErrorResponse>}.
 *
 * <pre>
 * MethodArgumentNotValidException        → 400  Bean Validation
 * HttpMessageNotReadableException        → 400  JSON mal formado / enum inexistente
 * MethodArgumentTypeMismatchException    → 400  parámetro con tipo inválido
 * MissingServletRequestParameterException→ 400  falta un query param obligatorio
 * ResourceNotFoundException              → 404
 * DuplicateResourceException             → 409  conflicto de unicidad
 * BusinessRuleException                  → 409  regla de negocio incumplida
 * Exception (cualquier otra)             → 500
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ 404

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // ------------------------------------------------------------------ 409

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // ------------------------------------------------------------------ 400

    /** Bean Validation sobre el body (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                        (first, second) -> first));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /** JSON mal formado o valor de enum inexistente en el body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    /** Path variable o query param que no se puede convertir al tipo esperado (ej. artistId = "abc"). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = Map.of(
                ex.getName(), "Invalid value: " + ex.getValue());

        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter", details);
    }

    /** Falta un query param obligatorio (ej. GET /api/events/by-artist sin stageName). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                Map.of(ex.getParameterName(), "Parameter is required"));
    }

    // ------------------------------------------------- errores de enrutamiento
    // Sin estos dos handlers, el handler genérico de abajo convertiría una URL
    // inexistente o un método HTTP no permitido en un 500 engañoso.

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported for this endpoint",
                Map.of("method", String.valueOf(ex.getMethod())));
    }

    // ------------------------------------------------------------------ 500

    /** Error inesperado. Nunca se expone stack trace, SQL ni detalles internos (NFR-CTRL-007). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
    }

    // -------------------------------------------------------------- helper

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);

        return ResponseEntity.status(status).body(body);
    }
}

package com.pulsepass.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce a JSON las excepciones de seguridad que ocurren DENTRO de un controller/service
 * (por ejemplo cuando @PreAuthorize deniega).
 *
 * @Order(HIGHEST_PRECEDENCE): debe ir ANTES de tu GlobalExceptionHandler. Si él tiene un
 * @ExceptionHandler(Exception.class), capturaría AccessDeniedException y devolvería 500.
 *
 * Mantiene el contrato de ErrorResponse: timestamp, status, error, message, details.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        // @PreAuthorize también deniega a anónimos: en ese caso lo correcto es 401, no 403.
        if (isAnonymous()) {
            return body(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o ausentes");
        }
        return body(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        // Login fallido: mismo mensaje para "usuario no existe", "clave mala" o "usuario inactivo".
        return body(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o ausentes");
    }

    @ExceptionHandler(HiddenResourceException.class)
    public ResponseEntity<Map<String, Object>> handleHidden(HiddenResourceException ex) {
        return body(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPassword(WeakPasswordException ex) {
        Map<String, String> details = Map.of("password", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "Solicitud inválida", details);
    }

    private boolean isAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return body(status, message, Map.of());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Map<String, String> details) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", LocalDateTime.now().toString());
        map.put("status", status.value());
        map.put("error", status.getReasonPhrase());
        map.put("message", message);
        map.put("details", details);
        return ResponseEntity.status(status).body(map);
    }
}

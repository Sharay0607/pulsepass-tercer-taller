package com.pulsepass.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe el MISMO contrato JSON de ErrorResponse (timestamp, status, error, message, details)
 * para errores que ocurren ANTES de DispatcherServlet (filtros de seguridad), donde
 * @RestControllerAdvice no llega.
 *
 * Se arma a mano (sin ObjectMapper) a propósito: Spring Boot 4 usa Jackson 3 por defecto y
 * así este código no depende de qué versión de Jackson tengas en el classpath.
 * Los mensajes son constantes nuestras, nunca texto del cliente.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return; // la respuesta se emite UNA sola vez
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        String body = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + status.getReasonPhrase() + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"details\":{}}";
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}

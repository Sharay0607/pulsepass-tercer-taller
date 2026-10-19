package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato único de error de la API (PRD §13): todos los errores tienen esta estructura.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}

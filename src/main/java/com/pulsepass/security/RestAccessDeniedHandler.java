package com.pulsepass.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 403 Forbidden: "sé quién eres, pero no tienes permiso".
 * Registra la denegación (NFR-SEC006) sin datos sensibles.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    public static final String MESSAGE = "No tiene permisos para realizar esta operación";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object who = auth != null && auth.getPrincipal() instanceof SecurityPrincipal p ? p.getUserId() : "anon";
        log.warn("Acceso denegado userId={} {} {}", who, request.getMethod(), request.getRequestURI());
        JsonErrorWriter.write(response, HttpStatus.FORBIDDEN, MESSAGE);
    }
}

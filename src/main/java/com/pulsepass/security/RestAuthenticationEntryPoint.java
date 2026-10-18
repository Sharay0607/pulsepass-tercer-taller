package com.pulsepass.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 Unauthorized: "no sé quién eres" (sin token, token vencido, firma alterada...).
 * El mensaje es SIEMPRE el mismo para no filtrar por qué falló (SEC009).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String MESSAGE = "Credenciales inválidas o ausentes";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}

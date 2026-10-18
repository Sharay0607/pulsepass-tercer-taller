package com.pulsepass.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro Bearer único por request.
 *
 * Reglas:
 *  - Sin header Authorization            -> sigue la cadena como anónimo (las reglas de ruta deciden).
 *  - Más de un header Authorization      -> 401 (sospechoso).
 *  - Esquema distinto de Bearer          -> sigue como anónimo (httpBasic está deshabilitado).
 *  - Bearer inválido/vencido/usuario inactivo -> 401 inmediato (SEC008): jamás se autentica "por accidente".
 *  - Bearer válido -> se RECARGA el principal desde BD (roles y estado actuales) y se fija el contexto.
 *
 * NO es un @Component a propósito: si lo fuera, Spring Boot lo registraría TAMBIÉN como filtro
 * del contenedor (se ejecutaría fuera de la cadena de seguridad). SecurityConfig lo instancia.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint entryPoint;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
                                   AuthenticationEntryPoint entryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        if (headers.size() > 1) {
            reject(request, response, "múltiples headers Authorization");
            return;
        }

        String header = headers.get(0);
        if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        if (alreadyAuthenticated()) {
            chain.doFilter(request, response); // no sobrescribir una autenticación ya establecida
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        SecurityPrincipal principal;
        try {
            JwtService.TokenData data = jwtService.parse(token);
            principal = userDetailsService.loadUserById(data.userId());
            if (!principal.isEnabled()) {
                throw new DisabledException("Usuario inactivo");
            }
        } catch (InvalidJwtException | AuthenticationException e) {
            reject(request, response, e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(detailsSource.buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // IMPORTANTE: doFilter fuera del try, para no capturar excepciones del negocio como "token inválido".
        chain.doFilter(request, response);
    }

    private boolean alreadyAuthenticated() {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        return current != null && current.isAuthenticated() && !(current instanceof AnonymousAuthenticationToken);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        // Solo debug y sin el token: el motivo exacto NO se le dice al cliente.
        log.debug("JWT rechazado ({} {}): {}", request.getMethod(), request.getRequestURI(), reason);
        entryPoint.commence(request, response, new BadCredentialsException("Invalid bearer token"));
    }
}

package com.pulsepass.service;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.pulsepass.exception.HiddenResourceException;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.security.SecurityQueryRepository;

/**
 * Políticas de AUTORIZACIÓN POR PROPIEDAD (anti-IDOR). Se invoca desde SpEL:
 *
 *     @PreAuthorize("@authz.canViewTicket(#p0)")
 *
 * El bean se llama "authz". "#p0" = primer parámetro del método (no depende del nombre).
 *
 * Reglas de oro:
 *  1) El propietario se lee de la BASE DE DATOS, no del request.
 *  2) Se compara por ID persistente del principal, no por strings de email enviados por el cliente.
 *  3) Ante la duda, DENEGAR (devuelve false).
 *
 * Decisión de diseño (PRD 20.1): un recurso ajeno responde 403 SIEMPRE (también si no existe, para
 * no distinguir "no existe" de "es de otro"). Los borradores de evento responden 404 (ocultos).
 */
@Service("authz")
public class AuthorizationService {

    private static final String PUBLISHED = "PUBLISHED";

    private final SecurityQueryRepository queries;

    public AuthorizationService(SecurityQueryRepository queries) {
        this.queries = queries;
    }

    // --------------------------------------------------------------- contexto

    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return auth.getPrincipal() instanceof SecurityPrincipal p ? Optional.of(p) : Optional.empty();
    }

    public boolean isAdmin() {
        return currentPrincipal().map(p -> p.hasRole("ADMIN")).orElse(false);
    }

    /** STAFF o ADMIN. */
    public boolean isPrivileged() {
        return currentPrincipal().map(p -> p.hasRole("STAFF") || p.hasRole("ADMIN")).orElse(false);
    }

    // ---------------------------------------------------------------- usuarios

    public boolean isSelfByEmail(String email) {
        return currentPrincipal()
                .flatMap(p -> queries.findUserIdByEmail(email).map(id -> id.equals(p.getUserId())))
                .orElse(false);
    }

    public boolean isSelfByUsername(String username) {
        return currentPrincipal()
                .flatMap(p -> queries.findUserIdByUsername(username).map(id -> id.equals(p.getUserId())))
                .orElse(false);
    }

    /** GET /api/users/by-email y GET /api/tickets/by-user: el propio usuario, o ADMIN. */
    public boolean canAccessUserByEmail(String email) {
        return isAdmin() || isSelfByEmail(email);
    }

    /** GET /api/users/by-username: el propio usuario, o ADMIN. */
    public boolean canAccessUserByUsername(String username) {
        return isAdmin() || isSelfByUsername(username);
    }

    // ----------------------------------------------------------------- tickets

    /** POST /api/tickets: rol USER y el userEmail del DTO debe ser del principal. */
    public boolean canPurchaseFor(String userEmail) {
        boolean isUser = currentPrincipal().map(p -> p.hasRole("USER")).orElse(false);
        return isUser && isSelfByEmail(userEmail);
    }

    /** GET /api/tickets/{ticketCode}: propietario, o STAFF/ADMIN (necesitan verificar entradas). */
    public boolean canViewTicket(String ticketCode) {
        if (isPrivileged()) {
            return true; // si no existe, el Service responde 404 a ellos
        }
        return isTicketOwner(ticketCode);
    }

    /** PATCH /api/tickets/{ticketCode}/cancel: propietario, o ADMIN. */
    public boolean canCancelTicket(String ticketCode) {
        return isAdmin() || isTicketOwner(ticketCode);
    }

    private boolean isTicketOwner(String ticketCode) {
        return currentPrincipal()
                .flatMap(p -> queries.findTicketOwnerId(ticketCode).map(ownerId -> ownerId.equals(p.getUserId())))
                .orElse(false); // ticket inexistente => false => 403 uniforme
    }

    // ----------------------------------------------------------------- eventos

    /**
     * GET /api/events/{eventCode}: público SOLO si está publicado. Los borradores solo los ven
     * STAFF/ADMIN; para el resto se responde 404 (como si no existieran).
     */
    public boolean canViewEvent(String eventCode) {
        Optional<String> status = queries.findEventStatus(eventCode);
        if (status.isEmpty()) {
            return true; // no existe: que el Service lance su 404 habitual
        }
        if (PUBLISHED.equalsIgnoreCase(status.get()) || isPrivileged()) {
            return true;
        }
        throw new HiddenResourceException("Evento no disponible");
    }
}

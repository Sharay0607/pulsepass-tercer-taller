package com.pulsepass.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Consultas de SOLO LECTURA que necesita la capa de seguridad (SQL nativo vía JPA).
 *
 * ¿Por qué no usar tus repositorios/entidades de las Fases 1-3?
 *  - La seguridad queda DESACOPLADA del modelo de negocio.
 *  - Una sola consulta con JOIN trae identidad + hash + roles (evita N+1, NFR-SEC003).
 *
 * Los nombres de tablas y columnas corresponden al esquema real de V1 de PulsePass.
 */
@Repository
public class SecurityQueryRepository {

    private static final String ACCOUNT_SELECT = """
            SELECT u.id, u.username, u.email, u.active, c.password_hash,
                   COALESCE(string_agg(r.role, ','), '') AS roles
              FROM users u
              JOIN user_credentials c ON c.user_id = u.id
              LEFT JOIN user_roles r ON r.user_id = u.id
            """;

    private static final String ACCOUNT_GROUP =
            " GROUP BY u.id, u.username, u.email, u.active, c.password_hash";

    @PersistenceContext
    private EntityManager em;

    public record UserAccount(Long id, String username, String email, boolean active,
                              String passwordHash, Set<String> roles) {
        @Override
        public String toString() {
            return "UserAccount[id=" + id + ", username=" + username + ", active=" + active + "]";
        }
    }

    // ------------------------------------------------------------- identidad

    /**
     * Si el identificador contiene '@' se busca SOLO por email; si no, SOLO por username.
     * Evita que alguien registre un username igual al email de otra persona.
     * Solo devuelve usuarios que TIENEN credencial (los legados sin contraseña no pueden entrar).
     */
    public Optional<UserAccount> findAccountByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String column = identifier.contains("@") ? "email" : "username";
        String sql = ACCOUNT_SELECT + " WHERE lower(u." + column + ") = lower(:v)" + ACCOUNT_GROUP;
        return mapAccount(em.createNativeQuery(sql).setParameter("v", identifier.trim()).getResultList());
    }

    public Optional<UserAccount> findAccountById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String sql = ACCOUNT_SELECT + " WHERE u.id = :v" + ACCOUNT_GROUP;
        return mapAccount(em.createNativeQuery(sql).setParameter("v", userId).getResultList());
    }

    private static Optional<UserAccount> mapAccount(List<?> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] r = (Object[]) rows.get(0);
        String rolesCsv = (String) r[5];
        Set<String> roles = rolesCsv == null || rolesCsv.isBlank()
                ? Set.of()
                : Arrays.stream(rolesCsv.split(",")).collect(Collectors.toUnmodifiableSet());
        return Optional.of(new UserAccount(((Number) r[0]).longValue(), (String) r[1], (String) r[2],
                (Boolean) r[3], (String) r[4], roles));
    }

    // ------------------------------------------------------------ propiedad

    public Optional<Long> findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return firstLong("SELECT id FROM users WHERE lower(email) = lower(:v)", email.trim());
    }

    public Optional<Long> findUserIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return firstLong("SELECT id FROM users WHERE lower(username) = lower(:v)", username.trim());
    }

    /** El propietario SIEMPRE se lee de la base de datos, jamás del request (anti-IDOR). */
    public Optional<Long> findTicketOwnerId(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            return Optional.empty();
        }
        return firstLong("SELECT user_id FROM tickets WHERE ticket_code = :v", ticketCode);
    }

    public Optional<String> findEventStatus(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            return Optional.empty();
        }
        List<?> rows = em.createNativeQuery("SELECT status FROM events WHERE event_code = :v")
                .setParameter("v", eventCode).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((String) rows.get(0));
    }

    private Optional<Long> firstLong(String sql, String param) {
        List<?> rows = em.createNativeQuery(sql).setParameter("v", param).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
    }
}

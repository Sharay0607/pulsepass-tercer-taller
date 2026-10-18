package com.pulsepass.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credencial 1:1 con User (PRD 9.1). Vive en su PROPIA tabla para que el hash nunca pueda
 * serializarse por accidente desde un mapper de MapStruct del User.
 *
 * userId es un Long simple (sin @OneToOne): no dependemos de tu entidad User, y la integridad
 * la garantiza la FOREIGN KEY de la migración V4.
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    protected UserCredential() {
        // requerido por JPA
    }

    public UserCredential(Long userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    @Override
    public String toString() {
        return "UserCredential[userId=" + userId + "]"; // nunca el hash
    }
}

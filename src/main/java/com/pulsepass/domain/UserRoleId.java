package com.pulsepass.domain;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta (user_id, role) de la tabla user_roles. */
public class UserRoleId implements Serializable {

    private Long userId;
    private RoleName role;

    public UserRoleId() {
    }

    public UserRoleId(Long userId, RoleName role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public RoleName getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UserRoleId other && Objects.equals(userId, other.userId) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }
}

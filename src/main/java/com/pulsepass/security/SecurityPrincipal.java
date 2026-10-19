package com.pulsepass.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal: identidad comprobada de quien hace la solicitud.
 * Guarda el ID PERSISTENTE (userId), que es lo que se usa para comparar propiedad
 * (anti-IDOR), nunca el email enviado por el cliente.
 */
public class SecurityPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final Set<String> roles;
    private final Set<GrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String email, String passwordHash,
                             boolean active, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roles = Set.copyOf(roles);
        this.authorities = roles.stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static SecurityPrincipal from(SecurityQueryRepository.UserAccount account) {
        return new SecurityPrincipal(account.id(), account.username(), account.email(),
                account.passwordHash(), account.active(), account.roles());
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    /** Roles sin prefijo (USER, STAFF, ADMIN). */
    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Un usuario desactivado pierde acceso en el siguiente request (SEC011). */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SecurityPrincipal other && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    /** toString seguro: jamás imprime hash. */
    @Override
    public String toString() {
        return "SecurityPrincipal[userId=" + userId + ", username=" + username + ", roles=" + roles + "]";
    }
}

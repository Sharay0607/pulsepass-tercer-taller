package com.pulsepass.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga identidad + hash + roles + estado desde la base de datos.
 * Se usa en DOS momentos:
 *  1) Login: loadUserByUsername(identifier)  -> lo invoca DaoAuthenticationProvider.
 *  2) Cada request con JWT: loadUserById(sub) -> revalida estado y roles ACTUALES (SEC011).
 * Prohibido aquí: reglas comerciales.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SecurityQueryRepository repository;

    public CustomUserDetailsService(SecurityQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return repository.findAccountByIdentifier(identifier)
                .map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        return repository.findAccountById(userId)
                .map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}

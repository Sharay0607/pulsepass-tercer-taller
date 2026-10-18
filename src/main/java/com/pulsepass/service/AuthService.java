package com.pulsepass.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsepass.domain.RoleName;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.WeakPasswordException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;

/**
 * Orquesta autenticación y registro de credenciales. NO reemplaza a UserService:
 * las reglas de negocio del alta (unicidad, edad mínima, etc.) las sigue aplicando él.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MIN_PASSWORD_CHARS = 12;
    private static final int MAX_PASSWORD_BYTES = 72; // límite real de BCrypt

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UserService userService, PasswordEncoder passwordEncoder,
                       UserCredentialRepository credentialRepository, UserRoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * FR-SECAUTH-001: User + credencial BCrypt + ROLE_USER en UNA transacción.
     * Si cualquier paso falla, NADA queda guardado (no existen cuentas sin credenciales).
     * El rol inicial es SIEMPRE USER: el DTO no trae rol, así que nadie puede auto-promoverse.
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        validatePassword(request.password());

        // 1) Reglas de negocio de la Fase 2 (se une a ESTA transacción: propagación REQUIRED).
        UserResponse created = userService.register(request);

        // 2) UserResponse ya trae el ID persistente (el INSERT ya se ejecutó: id IDENTITY).
        Long userId = created.id();

        // 3) Credencial (hash BCrypt) + rol USER.
        credentialRepository.save(new UserCredential(userId, passwordEncoder.encode(request.password())));
        roleRepository.save(new UserRole(userId, RoleName.USER));

        log.info("registro correcto userId={}", userId);
        return created;
    }

    /** FR-SECAUTH-002/003/004. Cualquier fallo se reporta igual: 401 genérico. */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.identifier().trim(), request.password()));
            SecurityPrincipal principal = (SecurityPrincipal) auth.getPrincipal();

            JwtService.IssuedToken token = jwtService.issueToken(principal.getUserId());
            log.info("login correcto userId={}", principal.getUserId());
            return AuthResponse.bearer(token.value(), token.expiresInSeconds());

        } catch (AuthenticationException e) {
            // Se registra el TIPO de fallo (interno) pero al cliente solo le llega un 401 genérico.
            log.warn("login fallido identifier={} motivo={}", sanitize(request.identifier()), e.getClass().getSimpleName());
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    // ----------------------------------------------------------------- helpers

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_CHARS) {
            throw new WeakPasswordException("La contraseña debe tener al menos " + MIN_PASSWORD_CHARS + " caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new WeakPasswordException("La contraseña no puede superar " + MAX_PASSWORD_BYTES + " bytes");
        }
    }

    /** Evita "log injection": quita saltos de línea/control y recorta. */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replaceAll("[\\p{Cntrl}]", "_");
        return clean.length() > 50 ? clean.substring(0, 50) + "..." : clean;
    }
}

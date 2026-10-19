package com.pulsepass.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades JWT leídas de application.yml / variables de entorno.
 *
 * pulsepass.security.jwt.issuer            -> PULSEPASS_JWT_ISSUER (default pulsepass-api)
 * pulsepass.security.jwt.secret-base64     -> PULSEPASS_JWT_SECRET_BASE64 (OBLIGATORIA, sin default)
 * pulsepass.security.jwt.access-token-ttl  -> PT15M (ISO-8601)
 *
 * La validación fuerte (Base64 válido, >= 32 bytes) vive en JwtService para que la
 * aplicación FALLE AL ARRANCAR si la clave es débil (requisito SEC004 / PRD 12.2).
 */
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(String issuer, String secretBase64, Duration accessTokenTtl) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "pulsepass-api";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }

    /** Evita que la clave se imprima si alguien hace log del objeto. */
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secretBase64=***, accessTokenTtl=" + accessTokenTtl + "]";
    }
}

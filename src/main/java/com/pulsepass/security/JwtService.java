package com.pulsepass.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Responsabilidad ÚNICA: emitir y validar JWT (firma, issuer, algoritmo, fechas, sujeto).
 * PROHIBIDO aquí: consultar tickets o ejecutar negocio (PRD 7.1).
 */
@Service
public class JwtService {

    private static final String EXPECTED_ALG = "HS256";
    private static final int MIN_KEY_BYTES = 32;          // 256 bits
    private static final int MAX_TOKEN_LENGTH = 4096;     // defensa contra entradas gigantes
    private static final long MAX_IAT_FUTURE_SECONDS = 30; // tolerancia a reloj adelantado

    private final SecretKey key;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;
    private final JwtParser parser;

    @Autowired
    public JwtService(JwtProperties props) {
        this(props, Clock.systemUTC());
    }

    /** Constructor con Clock inyectable: permite probar expiración sin Thread.sleep. */
    public JwtService(JwtProperties props, Clock clock) {
        this.clock = clock;
        this.issuer = props.issuer();
        this.ttl = props.accessTokenTtl();

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl debe ser positivo");
        }
        if (props.secretBase64() == null || props.secretBase64().isBlank()) {
            throw new IllegalStateException("Falta PULSEPASS_JWT_SECRET_BASE64: la aplicación no puede arrancar sin clave JWT");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(props.secretBase64());
        } catch (RuntimeException e) {
            // Nunca incluir el valor de la clave en el mensaje.
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 no es Base64 válido");
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 es insuficiente: se requieren al menos 32 bytes aleatorios (openssl rand -base64 32)");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        // Parser reutilizable e inmutable (thread-safe). La clave es SecretKey => solo acepta HMAC.
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    // ------------------------------------------------------------------ emisión

    public IssuedToken issueToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId requerido");
        }
        Instant now = clock.instant();
        Instant exp = now.plus(ttl);

        String token = Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new IssuedToken(token, ttl.toSeconds());
    }

    // --------------------------------------------------------------- validación

    /**
     * Valida el token y devuelve los datos mínimos.
     * Lanza InvalidJwtException ante CUALQUIER problema (nunca devuelve datos parciales).
     */
    public TokenData parse(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidJwtException("Token vacío o con tamaño inaceptable");
        }
        try {
            // parseSignedClaims: exige JWS firmado. Rechaza tokens "unsigned" (alg=none)
            // y verifica la firma criptográficamente ANTES de entregarnos los claims.
            Jws<Claims> jws = parser.parseSignedClaims(token);

            if (!EXPECTED_ALG.equals(jws.getHeader().getAlgorithm())) {
                throw new InvalidJwtException("Algoritmo inesperado");
            }

            Claims claims = jws.getPayload();

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new InvalidJwtException("Falta sub");
            }
            if (claims.getExpiration() == null) {
                throw new InvalidJwtException("Falta exp");
            }
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw new InvalidJwtException("Falta jti");
            }
            Date iat = claims.getIssuedAt();
            if (iat == null) {
                throw new InvalidJwtException("Falta iat");
            }
            if (iat.toInstant().isAfter(clock.instant().plusSeconds(MAX_IAT_FUTURE_SECONDS))) {
                throw new InvalidJwtException("iat en el futuro");
            }

            long userId;
            try {
                userId = Long.parseLong(subject);
            } catch (NumberFormatException e) {
                throw new InvalidJwtException("sub no es un identificador válido");
            }

            return new TokenData(userId, claims.getId(), iat.toInstant(), claims.getExpiration().toInstant());

        } catch (InvalidJwtException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            // ExpiredJwtException, MalformedJwtException, SignatureException,
            // UnsupportedJwtException, etc. todos heredan de JwtException.
            throw new InvalidJwtException("Token inválido", e);
        }
    }

    // ----------------------------------------------------------------- records

    public record IssuedToken(String value, long expiresInSeconds) {
        @Override
        public String toString() {
            return "IssuedToken[value=***, expiresInSeconds=" + expiresInSeconds + "]";
        }
    }

    public record TokenData(Long userId, String jti, Instant issuedAt, Instant expiresAt) {
    }
}

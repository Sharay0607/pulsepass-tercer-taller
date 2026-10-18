package com.pulsepass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Prueba unitaria PURA (sin Spring, sin BD). Rápida y determinista. */
class JwtServiceTest {

    private static final String ISSUER = "pulsepass-test";
    private static final String KEY_A = b64("clave-de-prueba-A-0123456789-0123456789");   // 40 bytes
    private static final String KEY_B = b64("clave-de-prueba-B-9876543210-9876543210");   // 40 bytes

    private final JwtService service = new JwtService(props(ISSUER, KEY_A));

    private static String b64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static JwtProperties props(String issuer, String key) {
        return new JwtProperties(issuer, key, Duration.ofMinutes(15));
    }

    @Test
    void emiteYValidaTokenConClaimsCorrectos() {
        JwtService.IssuedToken issued = service.issueToken(42L);
        JwtService.TokenData data = service.parse(issued.value());

        assertThat(data.userId()).isEqualTo(42L);
        assertThat(data.jti()).isNotBlank();
        assertThat(issued.expiresInSeconds()).isEqualTo(900);
        assertThat(Duration.between(data.issuedAt(), data.expiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void cadaTokenTieneJtiDistinto() {
        String a = service.parse(service.issueToken(1L).value()).jti();
        String b = service.parse(service.issueToken(1L).value()).jti();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rechazaTokenExpirado() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        String expired = new JwtService(props(ISSUER, KEY_A), past).issueToken(1L).value();

        assertThatThrownBy(() -> service.parse(expired)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaFirmaAlterada() {
        String token = service.issueToken(1L).value();
        String[] parts = token.split("\\.");
        char original = parts[2].charAt(10);
        char changed = original == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "."
                + parts[2].substring(0, 10) + changed + parts[2].substring(11);

        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        String foreign = new JwtService(props(ISSUER, KEY_B)).issueToken(1L).value();
        assertThatThrownBy(() -> service.parse(foreign)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaIssuerErroneo() {
        String otherIssuer = new JwtService(props("otro-emisor", KEY_A)).issueToken(1L).value();
        assertThatThrownBy(() -> service.parse(otherIssuer)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaTokenSinFirmaAlgNone() {
        String unsigned = Jwts.builder()
                .subject("1").issuer(ISSUER).id("x")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(600)))
                .compact(); // sin signWith => alg=none

        assertThatThrownBy(() -> service.parse(unsigned)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaAlgoritmoDistintoDeHS256() {
        byte[] key64 = new byte[64];
        java.util.Arrays.fill(key64, (byte) 7);
        // Con una clave de 64 bytes JJWT firma con HS512; el servicio debe exigir HS256 igualmente.
        String hs512 = Jwts.builder()
                .subject("1").issuer(ISSUER).id("x")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(key64))
                .compact();
        JwtService svc = new JwtService(props(ISSUER, Base64.getEncoder().encodeToString(key64)));

        assertThatThrownBy(() -> svc.parse(hs512)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaTokenSinSubject() {
        String noSub = Jwts.builder()
                .issuer(ISSUER).id("x")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(KEY_A)), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> service.parse(noSub)).isInstanceOf(InvalidJwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "a.b.c", "Bearer xyz", "....", "eyJhbGciOiJub25lIn0.e30."})
    void rechazaEntradasMalformadas(String garbage) {
        assertThatThrownBy(() -> service.parse(garbage)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rechazaNulo() {
        assertThatThrownBy(() -> service.parse(null)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void laAplicacionFallaSiLaClaveEsDebil() {
        String weak = Base64.getEncoder().encodeToString("solo-16-bytes-xx".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new JwtService(props(ISSUER, weak)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void laAplicacionFallaSiFaltaLaClave() {
        assertThatThrownBy(() -> new JwtService(props(ISSUER, null))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(ISSUER, " "))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elMensajeDeErrorNoRevelaLaClave() {
        String weak = Base64.getEncoder().encodeToString("corta".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new JwtService(props(ISSUER, weak)))
                .hasMessageNotContaining(weak);
    }
}

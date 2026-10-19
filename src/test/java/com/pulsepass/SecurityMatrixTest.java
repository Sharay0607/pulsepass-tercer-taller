package com.pulsepass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Spring Boot 4: AutoConfigureMockMvc vive en el módulo webmvc-test.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Matriz de seguridad con la cadena de filtros REAL (no se usa addFilters=false).
 * Requiere Docker (Testcontainers). Se mockea SOLO CustomUserDetailsService para no
 * depender de filas en BD: la firma y los filtros sí son los de verdad.
 *
 * Hereda de AbstractIntegrationTest (contenedor postgres:16 y perfil "test" con la clave JWT de pruebas).
 */
@AutoConfigureMockMvc
class SecurityMatrixTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Value("${pulsepass.security.jwt.secret-base64}")
    String secretBase64;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private static SecurityPrincipal principal(long id, boolean active, String... roles) {
        return new SecurityPrincipal(id, "user" + id, "user" + id + "@x.com", "hash", active, Set.of(roles));
    }

    @BeforeEach
    void stubUsers() {
        when(userDetailsService.loadUserById(1L)).thenReturn(principal(1L, true, "USER"));
        when(userDetailsService.loadUserById(2L)).thenReturn(principal(2L, true, "STAFF"));
        when(userDetailsService.loadUserById(3L)).thenReturn(principal(3L, true, "ADMIN"));
        when(userDetailsService.loadUserById(4L)).thenReturn(principal(4L, false, "USER")); // inactivo
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueToken(userId).value();
    }

    private static ResultMatcher notUnauthorizedNorForbidden() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------------------------------------------------------------- públicas
    @Test
    void anonimoPuedeListarEventosPublicados() throws Exception {
        mvc.perform(get("/api/events/published")).andExpect(status().isOk());
    }

    @Test
    void anonimoPuedeConsultarVenuesActivos() throws Exception {
        mvc.perform(get("/api/venues/active")).andExpect(notUnauthorizedNorForbidden());
    }

    // -------------------------------------------------------- denegar por defecto
    @Test
    void rutaInventadaSinTokenEs401NoPublica() throws Exception {
        mvc.perform(get("/api/ruta-inventada")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------- 401/403
    @Test
    void anonimoNoPuedeCrearEvento_401_conContratoErrorResponse() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void userNoPuedeCrearEvento_403() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));
    }

    @Test
    void adminPasaElFiltroDeSeguridadAlCrearEvento() throws Exception {
        // El cuerpo {} es inválido a propósito: esperamos 400 de Bean Validation (NO 401/403).
        mvc.perform(post("/api/events").header("Authorization", bearer(3L))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(notUnauthorizedNorForbidden());
    }

    @Test
    void userNoPuedePublicarEvento_403() throws Exception {
        mvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", bearer(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userNoPuedeMarcarTicketComoUsado_403_yStaffSiPasaElFiltro() throws Exception {
        mvc.perform(patch("/api/tickets/NOEXISTE/use").header("Authorization", bearer(1L)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/tickets/NOEXISTE/use").header("Authorization", bearer(2L)))
                .andExpect(notUnauthorizedNorForbidden());
    }

    @Test
    void userNoPuedeListarTicketsPagadosDeUnEvento_403() throws Exception {
        mvc.perform(get("/api/events/CMF-2026/tickets/paid").header("Authorization", bearer(1L)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ tokens
    @Test
    void tokenBasuraEs401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConFirmaAlteradaEs401() throws Exception {
        String token = jwtService.issueToken(1L).value();
        String[] p = token.split("\\.");
        char c = p[2].charAt(10) == 'A' ? 'B' : 'A';
        String tampered = p[0] + "." + p[1] + "." + p[2].substring(0, 10) + c + p[2].substring(11);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenExpiradoEs401() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder()
                .subject("1").issuer("pulsepass-test").id("jti-x")
                .issuedAt(Date.from(past.minusSeconds(60))).expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64)), Jwts.SIG.HS256)
                .compact();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDeUsuarioInactivoEs401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(4L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoEnRutaPublicaTambienEs401() throws Exception {
        mvc.perform(get("/api/events/published").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------------------- /me
    @Test
    void meSinTokenEs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meConTokenValidoDevuelveSoloDatosMinimos() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ------------------------------------------------------------------- login
    @Test
    void loginConCuerpoVacioEs400PorBeanValidation() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

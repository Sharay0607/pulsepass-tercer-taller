package com.pulsepass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pulsepass.exception.HiddenResourceException;
import com.pulsepass.service.AuthorizationService;

/** Políticas anti-IDOR: dueño vs tercero, STAFF, ADMIN, borradores. */
class AuthorizationServiceTest {

    private SecurityQueryRepository queries;
    private AuthorizationService authz;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        queries = mock(SecurityQueryRepository.class);
        authz = new AuthorizationService(queries);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id, String... roles) {
        SecurityPrincipal p = new SecurityPrincipal(id, "u" + id, "u" + id + "@x.com", "hash", true, Set.of(roles));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    }

    // ------------------------------------------------------------- tickets
    @Test
    void propietarioPuedeVerSuTicket() {
        loginAs(1, "USER");
        when(queries.findTicketOwnerId("T-1")).thenReturn(Optional.of(1L));
        assertThat(authz.canViewTicket("T-1")).isTrue();
    }

    @Test
    void terceroNoPuedeVerTicketAjeno() {
        loginAs(1, "USER");
        when(queries.findTicketOwnerId("T-CARLOS")).thenReturn(Optional.of(2L));
        assertThat(authz.canViewTicket("T-CARLOS")).isFalse();
    }

    @Test
    void ticketInexistenteSeDeniegaIgualQueAjeno() {
        loginAs(1, "USER");
        when(queries.findTicketOwnerId("NOPE")).thenReturn(Optional.empty());
        assertThat(authz.canViewTicket("NOPE")).isFalse();
    }

    @Test
    void staffPuedeVerCualquierTicket() {
        loginAs(5, "STAFF");
        assertThat(authz.canViewTicket("T-CARLOS")).isTrue();
    }

    @Test
    void userNoPuedeCancelarTicketAjenoPeroAdminSi() {
        when(queries.findTicketOwnerId("T-CARLOS")).thenReturn(Optional.of(2L));
        loginAs(1, "USER");
        assertThat(authz.canCancelTicket("T-CARLOS")).isFalse();

        loginAs(9, "ADMIN");
        assertThat(authz.canCancelTicket("T-CARLOS")).isTrue();
    }

    @Test
    void staffNoPuedeCancelar() {
        loginAs(5, "STAFF");
        when(queries.findTicketOwnerId("T-CARLOS")).thenReturn(Optional.of(2L));
        assertThat(authz.canCancelTicket("T-CARLOS")).isFalse();
    }

    @Test
    void anonimoSiempreEsDenegado() {
        when(queries.findTicketOwnerId("T-1")).thenReturn(Optional.of(1L));
        assertThat(authz.canViewTicket("T-1")).isFalse();
        assertThat(authz.canCancelTicket("T-1")).isFalse();
        assertThat(authz.canPurchaseFor("a@x.com")).isFalse();
    }

    // -------------------------------------------------------------- compra
    @Test
    void userSoloPuedeComprarParaSuPropioEmail() {
        loginAs(1, "USER");
        when(queries.findUserIdByEmail("andrea@x.com")).thenReturn(Optional.of(1L));
        when(queries.findUserIdByEmail("carlos@x.com")).thenReturn(Optional.of(2L));

        assertThat(authz.canPurchaseFor("andrea@x.com")).isTrue();
        assertThat(authz.canPurchaseFor("carlos@x.com")).isFalse();
    }

    @Test
    void emailInexistenteNoPermiteComprar() {
        loginAs(1, "USER");
        when(queries.findUserIdByEmail("fantasma@x.com")).thenReturn(Optional.empty());
        assertThat(authz.canPurchaseFor("fantasma@x.com")).isFalse();
    }

    @Test
    void staffSinRolUserNoCompra() {
        loginAs(5, "STAFF");
        when(queries.findUserIdByEmail("laura@x.com")).thenReturn(Optional.of(5L));
        assertThat(authz.canPurchaseFor("laura@x.com")).isFalse();
    }

    // ------------------------------------------------------------- usuarios
    @Test
    void busquedaDeUsuarioSoloPropioOAdmin() {
        when(queries.findUserIdByEmail("carlos@x.com")).thenReturn(Optional.of(2L));
        loginAs(1, "USER");
        assertThat(authz.canAccessUserByEmail("carlos@x.com")).isFalse();

        loginAs(9, "ADMIN");
        assertThat(authz.canAccessUserByEmail("carlos@x.com")).isTrue();
    }

    // -------------------------------------------------------------- eventos
    @Test
    void eventoPublicadoEsVisibleParaTodos() {
        when(queries.findEventStatus("CMF-2026")).thenReturn(Optional.of("PUBLISHED"));
        assertThat(authz.canViewEvent("CMF-2026")).isTrue(); // anónimo
    }

    @Test
    void borradorSeOcultaConNotFoundParaAnonimoYUser() {
        when(queries.findEventStatus("DRAFT-1")).thenReturn(Optional.of("DRAFT"));
        assertThatThrownBy(() -> authz.canViewEvent("DRAFT-1")).isInstanceOf(HiddenResourceException.class);

        loginAs(1, "USER");
        assertThatThrownBy(() -> authz.canViewEvent("DRAFT-1")).isInstanceOf(HiddenResourceException.class);
    }

    @Test
    void borradorEsVisibleParaStaffYAdmin() {
        when(queries.findEventStatus("DRAFT-1")).thenReturn(Optional.of("DRAFT"));
        loginAs(5, "STAFF");
        assertThat(authz.canViewEvent("DRAFT-1")).isTrue();
        loginAs(9, "ADMIN");
        assertThat(authz.canViewEvent("DRAFT-1")).isTrue();
    }

    @Test
    void eventoInexistenteDejaQueElServiceLanceSu404() {
        when(queries.findEventStatus("NOPE")).thenReturn(Optional.empty());
        assertThat(authz.canViewEvent("NOPE")).isTrue();
    }
}

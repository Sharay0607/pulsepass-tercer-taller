package com.pulsepass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private CustomUserDetailsService users;
    private AuthenticationEntryPoint entryPoint;
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = mock(JwtService.class);
        users = mock(CustomUserDetailsService.class);
        entryPoint = mock(AuthenticationEntryPoint.class);
        filter = new JwtAuthenticationFilter(jwtService, users, entryPoint);
        request = new MockHttpServletRequest("GET", "/api/tickets/T-1");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static SecurityPrincipal principal(long id, boolean active, String... roles) {
        return new SecurityPrincipal(id, "user" + id, "user" + id + "@x.com", "hash", active, Set.of(roles));
    }

    private static JwtService.TokenData tokenOf(long userId) {
        return new JwtService.TokenData(userId, "jti", Instant.now(), Instant.now().plusSeconds(600));
    }

    @Test
    void sinHeaderContinuaComoAnonimo() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, entryPoint);
    }

    @Test
    void bearerValidoAutenticaConDatosActualesDeBD() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.parse("token-valido")).thenReturn(tokenOf(7L));
        SecurityPrincipal p = principal(7L, true, "USER");
        when(users.loadUserById(7L)).thenReturn(p);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(p);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_USER");
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void bearerInvalidoDevuelve401YNoContinua() throws Exception {
        request.addHeader("Authorization", "Bearer basura");
        when(jwtService.parse(anyString())).thenThrow(new InvalidJwtException("malo"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usuarioInactivoConTokenVigenteEsRechazado() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.parse("token-valido")).thenReturn(tokenOf(9L));
        when(users.loadUserById(9L)).thenReturn(principal(9L, false, "USER"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usuarioEliminadoConTokenVigenteEsRechazado() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.parse("token-valido")).thenReturn(tokenOf(11L));
        when(users.loadUserById(11L)).thenThrow(new UsernameNotFoundException("no existe"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void dobleHeaderAuthorizationEsSospechosoYSeRechaza() throws Exception {
        request.addHeader("Authorization", "Bearer uno");
        request.addHeader("Authorization", "Bearer dos");

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        verifyNoInteractions(jwtService);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void esquemaDistintoDeBearerNoSeProcesa() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, entryPoint);
    }

    @Test
    void noSobrescribeUnaAutenticacionYaEstablecida() throws Exception {
        TestingAuthenticationToken existing = new TestingAuthenticationToken("previo", "x", "ROLE_USER");
        existing.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService);
    }
}

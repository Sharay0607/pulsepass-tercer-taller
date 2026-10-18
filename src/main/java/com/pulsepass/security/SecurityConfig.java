package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Declara filtro, autenticación y reglas de ruta. PROHIBIDO: negocio de compra/publicación.
 *
 * ORDEN DE LOS MATCHERS: Spring evalúa de arriba hacia abajo y gana el PRIMERO que coincide.
 * Por eso van primero las rutas específicas y privilegiadas, y al final anyRequest().
 *
 * Esta tabla cubre el nivel "ROL". El nivel "PROPIEDAD" (ticket ajeno, datos de otro usuario,
 * borradores de eventos) se aplica con @PreAuthorize en los Services (ver AuthorizationService).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // costo 12: ~250 ms por hash, razonable para 2026
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        // Spring Security 7: el UserDetailsService va en el constructor (el no-arg fue removido).
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtService jwtService,
                                            CustomUserDetailsService userDetailsService,
                                            RestAuthenticationEntryPoint authEntryPoint,
                                            RestAccessDeniedHandler deniedHandler) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, authEntryPoint);

        http
            // CSRF protege contra cookies que el navegador envía solo. Aquí NO hay cookies de sesión:
            // el token va en un header que el navegador no añade automáticamente.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)   // -> 401 JSON
                .accessDeniedHandler(deniedHandler))        // -> 403 JSON
            .authorizeHttpRequests(auth -> auth
                // Boot redirige los errores internos a /error; sin esto un 404/500 se vería como 401.
                .requestMatchers("/error").permitAll()

                // --- Públicas de autenticación ---
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()

                // --- Privilegiadas (ANTES de los patrones públicos con comodín) ---
                .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")

                // --- Públicas de lectura ---
                // /api/events/{eventCode} y /by-artist quedan abiertas a nivel de URL, pero el Service
                // oculta los borradores (canViewEvent / filtro por publicados).
                .requestMatchers(HttpMethod.GET, "/api/events/published", "/api/events/by-artist", "/api/events/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/artists/**", "/api/venues/**").permitAll()

                // --- Denegar por defecto (SEC001) ---
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

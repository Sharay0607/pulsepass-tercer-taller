package com.pulsepass.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad SOLO para los tests de contrato de los controllers (Fase 3).
 * Los filtros siguen activos (no se usa addFilters=false): la identidad se configura con
 * @WithMockUser en cada test. Las reglas reales de rol/propiedad se prueban en SecurityMatrixTest.
 */
@TestConfiguration
public class ControllerTestSecurityConfig {

    @Bean
    SecurityFilterChain controllerTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}

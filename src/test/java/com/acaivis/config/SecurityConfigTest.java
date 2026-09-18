package com.acaivis.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void devePermitirDominiosDeProducao() {
        CorsConfigurationSource source = new SecurityConfig().cors();

        CorsConfiguration configuration =
                source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://acaivis.com.br"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://adm.acaivis.com.br"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://xn--aavis-yra7b.com.br"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://adm.xn--aavis-yra7b.com.br"));
    }

    @Test
    void devePermitirAmbienteLocal() {
        CorsConfigurationSource source = new SecurityConfig().cors();

        CorsConfiguration configuration =
                source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOriginPatterns().contains("http://localhost:*"));
    }

    @Test
    void devePermitirCredenciaisEMetodosNecessarios() {
        CorsConfigurationSource source = new SecurityConfig().cors();

        CorsConfiguration configuration =
                source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowCredentials());
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedMethods().contains("PUT"));
        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertTrue(configuration.getAllowedHeaders().contains("*"));
    }
}

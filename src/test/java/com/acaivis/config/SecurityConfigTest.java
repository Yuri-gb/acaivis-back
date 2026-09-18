package com.acaivis.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void devePermitirDominiosPublicos() {
        CorsConfiguration configuration = getConfiguration();

        assertTrue(configuration.getAllowedOriginPatterns().contains("https://acaivis.com.br"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://www.acaivis.com.br"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://acaivis-front-whh6.vercel.app"));
    }

    @Test
    void devePermitirAmbienteLocal() {
        CorsConfiguration configuration = getConfiguration();

        assertTrue(configuration.getAllowedOriginPatterns().contains("http://localhost:*"));
    }

    @Test
    void devePermitirCredenciaisEMetodosNecessarios() {
        CorsConfiguration configuration = getConfiguration();

        assertTrue(configuration.getAllowCredentials());
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedMethods().contains("PUT"));
        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertTrue(configuration.getAllowedHeaders().contains("*"));
    }

    private CorsConfiguration getConfiguration() {
        CorsConfigurationSource source = new SecurityConfig().cors();
        CorsConfiguration configuration =
                source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(configuration);
        return configuration;
    }
}

package com.greysoft.jbpm_engine.config;

import com.greysoft.jbpm_engine.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for creating a single shared AuthService bean.
 * Uses the property-platform-backend-services client for all backend service calls.
 */
@Configuration
public class AuthServiceConfig {

    @Bean
    public AuthService authService(
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret) {
        return new AuthService(tokenUri, clientId, clientSecret, "backend-services");
    }
}

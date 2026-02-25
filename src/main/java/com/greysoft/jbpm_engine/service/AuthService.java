package com.greysoft.jbpm_engine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

/**
 * Generic authentication service for obtaining JWT tokens from Keycloak
 * for service-to-service communication using client credentials grant.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final WebClient webClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String serviceName;

    private String cachedToken;
    private Instant tokenExpiryTime;

    public AuthService(String tokenUri, String clientId, String clientSecret, String serviceName) {
        this.webClient = WebClient.builder().build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.serviceName = serviceName;
    }

    public String getAccessToken() {
        if (isTokenValid()) {
            logger.debug("Using cached token for {}", serviceName);
            return cachedToken;
        }
        return getToken();
    }

    @SuppressWarnings("unchecked")
    private String getToken() {
        try {
            logger.info("Obtaining new access token for {} from Keycloak", serviceName);

            Map<String, Object> response = webClient.post()
                    .uri(tokenUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("grant_type=client_credentials" +
                            "&client_id=" + clientId +
                            "&client_secret=" + clientSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException("Failed to obtain token for " + serviceName + ": Invalid response from Keycloak");
            }

            cachedToken = (String) response.get("access_token");
            Integer expiresIn = (Integer) response.get("expires_in");

            tokenExpiryTime = Instant.now().plusSeconds(expiresIn - 30);

            logger.info("Successfully obtained access token for {}. Expires in {} seconds", serviceName, expiresIn);
            return cachedToken;

        } catch (Exception e) {
            logger.error("Failed to obtain access token for {} from Keycloak: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to obtain token for " + serviceName + " from Keycloak", e);
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null
            && tokenExpiryTime != null
            && Instant.now().isBefore(tokenExpiryTime);
    }
}

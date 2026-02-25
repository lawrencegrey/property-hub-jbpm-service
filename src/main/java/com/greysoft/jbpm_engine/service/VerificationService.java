package com.greysoft.jbpm_engine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final WebClient webClient;
    private final String apiKey;

    public VerificationService(
            @Value("${verification.service.url}") String verificationApiUrl,
            @Value("${verification.service.api-key}") String apiKey) {
        this.webClient = WebClient.builder().baseUrl(verificationApiUrl).build();
        this.apiKey = apiKey;
    }

    public Map<String, Object> verifyPassport(byte[] fileBytes, String filename) {
        return uploadForVerification("/verification/passport", fileBytes, filename);
    }

    public Map<String, Object> verifyGovernmentId(byte[] fileBytes, String filename) {
        return uploadForVerification("/verification/government-id", fileBytes, filename);
    }

    public Map<String, Object> verifyProofOfAddress(byte[] fileBytes, String filename) {
        return uploadForVerification("/verification/proof-of-address", fileBytes, filename);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> uploadForVerification(String uri, byte[] fileBytes, String filename) {
        try {
            logger.info("Uploading file '{}' for verification to {}", filename, uri);
            Map<String, Object> result = webClient.post()
                    .uri(uri)
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(fileBytes)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            logger.info("Verification result for '{}': {}", filename, result);
            return result;
        } catch (Exception e) {
            logger.error("Error verifying file '{}': {}", filename, e.getMessage(), e);
            throw new RuntimeException("Verification failed for " + filename, e);
        }
    }

    public boolean isServiceUp() {
        try {
            webClient.get()
                    .uri("/health")
                    .header("X-API-KEY", apiKey)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("[isServiceUp] Verification API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

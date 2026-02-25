package com.greysoft.jbpm_engine.service;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final WebClient webClient;
    private final String documentApiUrl;
    private final AuthService authService;

    public DocumentService(
            @Value("${document.service.url}") String documentApiUrl,
            AuthService authService) {
        this.webClient = WebClient.builder().baseUrl(documentApiUrl).build();
        this.documentApiUrl = documentApiUrl;
        this.authService = authService;
    }

    private String getAuthorizationHeader() {
        return "Bearer " + authService.getAccessToken();
    }

    public Optional<DocumentDto> getDocumentById(UUID id) {
        try {
            logger.info("Fetching document by id: {}", id);
            DocumentDto doc = webClient.get()
                    .uri("/documents/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(DocumentDto.class)
                    .block();
            return Optional.ofNullable(doc);
        } catch (Exception e) {
            logger.error("Error fetching document {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<DocumentDto> updateDocument(UUID id, DocumentDto document) {
        try {
            logger.info("Updating document: {}", id);
            DocumentDto updated = webClient.put()
                    .uri("/documents/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(document)
                    .retrieve()
                    .bodyToMono(DocumentDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating document {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deleteDocument(UUID id) {
        try {
            webClient.delete()
                    .uri("/documents/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting document {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    public Integer deleteDocumentsByPropertyId(UUID propertyId) {
        try {
            logger.info("Deleting documents for propertyId: {}", propertyId);
            Integer count = webClient.delete()
                    .uri("/documents/property/{propertyId}", propertyId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error deleting documents for property {}: {}", propertyId, e.getMessage(), e);
            return 0;
        }
    }

    public Integer deleteDocumentsByPersonId(UUID personId) {
        try {
            logger.info("Deleting documents for personId: {}", personId);
            Integer count = webClient.delete()
                    .uri("/documents/person/{personId}", personId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error deleting documents for person {}: {}", personId, e.getMessage(), e);
            return 0;
        }
    }

    public byte[] getFile(UUID documentId) {
        try {
            logger.info("Downloading file for documentId: {}", documentId);
            byte[] fileBytes = webClient.get()
                    .uri("/documents/{id}/download", documentId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            return fileBytes;
        } catch (Exception e) {
            logger.error("Error downloading file for document {}: {}", documentId, e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void updateDocumentVerification(UUID documentId, String method, Object verificationResult, boolean isVerified) {
        try {
            logger.info("Updating verification for documentId={}, method={}, isVerified={}", documentId, method, isVerified);
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("verification_method", method);
            payload.put("verification_result", verificationResult);
            payload.put("is_verified", isVerified);
            webClient.put()
                    .uri("/documents/{id}/verification", documentId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            logger.error("Error updating verification for document {}: {}", documentId, e.getMessage(), e);
        }
    }

    public boolean isServiceUp() {
        try {
            webClient.get()
                    .uri("/health")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("[isServiceUp] Document API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

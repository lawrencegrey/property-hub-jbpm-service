package com.greysoft.jbpm_engine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final WebClient webClient;
    private final AuthService authService;

    public NotificationService(
            @Value("${notification.service.url}") String notificationApiUrl,
            AuthService authService) {
        this.webClient = WebClient.builder().baseUrl(notificationApiUrl).build();
        this.authService = authService;
    }

    private String getAuthorizationHeader() {
        return "Bearer " + authService.getAccessToken();
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            logger.info("Sending email to: {}", to);
            Map<String, Object> payload = Map.of(
                    "to", List.of(to),
                    "subject", subject,
                    "body", body,
                    "html_content", "",
                    "cc", List.of(),
                    "bcc", List.of(),
                    "attachments", List.of()
            );
            webClient.post()
                    .uri("/email/send")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", to, e.getMessage(), e);
        }
    }

    public Map<String, Object> sendEmailStructured(List<String> to, String subject, String body) {
        try {
            logger.info("Sending structured email to: {}", to);
            Map<String, Object> payload = Map.of(
                    "to", to,
                    "subject", subject,
                    "body", body,
                    "html_content", "",
                    "cc", List.of(),
                    "bcc", List.of(),
                    "attachments", List.of()
            );
            webClient.post()
                    .uri("/email/send")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("Email sent successfully to: {}", to);
            return Map.of("success", true);
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", to, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    public void sendSms(String to, String message) {
        try {
            logger.info("Sending SMS to: {}", to);
            Map<String, String> payload = Map.of(
                    "to", to,
                    "message", message
            );
            webClient.post()
                    .uri("/sms/send")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("SMS sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending SMS to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendPushNotification(String deviceToken, String title, String body) {
        try {
            logger.info("Sending push notification to device: {}", deviceToken);
            Map<String, String> payload = Map.of(
                    "device_token", deviceToken,
                    "title", title,
                    "body", body
            );
            webClient.post()
                    .uri("/pushed-notification/send")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("Push notification sent to device: {}", deviceToken);
        } catch (Exception e) {
            logger.error("Error sending push notification: {}", e.getMessage(), e);
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
            logger.warn("[isServiceUp] Notification API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

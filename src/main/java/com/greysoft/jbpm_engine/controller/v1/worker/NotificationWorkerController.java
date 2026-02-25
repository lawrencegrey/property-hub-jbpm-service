package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST endpoints for notification worker operations.
 * Called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker methods:
 *  - submit-report, notify-admins, send-sms-notification
 *  - send-email-notification, send-pushed-notification
 *  - send-user-message, populate-doc-notification
 */
@RestController
@RequestMapping("/api/v1/workers/notification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Worker", description = "REST endpoints for notification workflow operations (called by jBPM)")
public class NotificationWorkerController {

    private final NotificationService notificationService;

    @Value("${property.platform.admin.emails}")
    private String adminEmails;

    @PostMapping("/submit-report")
    @Operation(summary = "Build a report body from variables and return it")
    public ResponseEntity<Map<String, Object>> submitReport(@RequestBody Map<String, Object> variables) {
        log.info("[submit-report] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String entity = (String) variables.getOrDefault("entity", "N/A");
            String issue = (String) variables.getOrDefault("issue", "N/A");
            String personId = String.valueOf(variables.getOrDefault("person_id", "N/A"));
            String professionalId = String.valueOf(variables.getOrDefault("professional_id", "N/A"));
            String propertyId = String.valueOf(variables.getOrDefault("property_id", "N/A"));
            String propertyName = (String) variables.getOrDefault("property_name", "N/A");
            String reporterId = (String) variables.getOrDefault("reporter_id", "N/A");
            String reporterName = (String) variables.getOrDefault("reporter_name", "N/A");

            String body = """
                    A client has submitted a report regarding the following: %s

                    Name/Item: %s
                    Issue Reported: %s

                    Reported By: %s (ID: %s)

                    Reference IDs:
                    • Property ID: %s
                    • Person ID: %s
                    • Professional ID: %s

                    Please review this report in the admin portal.
                    """.formatted(
                    entity, propertyName, issue,
                    reporterName, reporterId,
                    propertyId, personId, professionalId
            );

            result.put("body", body);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[submit-report] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/notify-admins")
    @Operation(summary = "Send email notification to all configured admin emails")
    public ResponseEntity<Map<String, Object>> notifyAdmins(@RequestBody Map<String, Object> variables) {
        log.info("[notify-admins] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String subject = (String) variables.get("subject");
            String body = (String) variables.get("body");

            if (adminEmails == null || adminEmails.trim().isEmpty()) {
                log.error("No admin emails configured in application properties");
                result.put("success", false);
                result.put("error", "No admin emails configured");
                return ResponseEntity.ok(result);
            }

            List<String> emailList = Arrays.asList(adminEmails.split(","));
            int sent = 0;
            for (String email : emailList) {
                String trimmed = email.trim();
                if (!trimmed.isEmpty()) {
                    notificationService.sendEmail(trimmed, subject, body);
                    sent++;
                }
            }

            result.put("success", true);
            result.put("emails_sent", sent);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[notify-admins] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/send-sms-notification")
    @Operation(summary = "Send an SMS notification")
    public ResponseEntity<Map<String, Object>> sendSmsNotification(@RequestBody Map<String, Object> variables) {
        log.info("[send-sms-notification] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String mobileNumber = (String) variables.get("mobile_number");
            String message = (String) variables.get("message");

            if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing mobile_number");
                return ResponseEntity.badRequest().body(result);
            }

            notificationService.sendSms(mobileNumber, message);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[send-sms-notification] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/send-email-notification")
    @Operation(summary = "Send an email notification")
    public ResponseEntity<Map<String, Object>> sendEmailNotification(@RequestBody Map<String, Object> variables) {
        log.info("[send-email-notification] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String email = (String) variables.get("email");
            String subject = (String) variables.get("subject");
            String body = (String) variables.get("body");

            if (email == null || email.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing email");
                return ResponseEntity.badRequest().body(result);
            }

            notificationService.sendEmail(email, subject, body);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[send-email-notification] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/send-pushed-notification")
    @Operation(summary = "Send a push notification")
    public ResponseEntity<Map<String, Object>> sendPushedNotification(@RequestBody Map<String, Object> variables) {
        log.info("[send-pushed-notification] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String deviceToken = (String) variables.get("device_token");
            String subject = (String) variables.get("subject");
            String body = (String) variables.get("body");

            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing device_token");
                return ResponseEntity.badRequest().body(result);
            }

            notificationService.sendPushNotification(deviceToken, subject, body);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[send-pushed-notification] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/send-user-message")
    @Operation(summary = "Send email, SMS, and push notification to a user")
    public ResponseEntity<Map<String, Object>> sendUserMessage(@RequestBody Map<String, Object> variables) {
        log.info("[send-user-message] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String email = (String) variables.get("email");
            String mobileNumber = (String) variables.get("mobile_number");
            String deviceToken = (String) variables.get("device_token");
            String subject = (String) variables.get("subject");
            String body = (String) variables.get("body");

            boolean emailSent = false;
            boolean smsSent = false;
            boolean pushSent = false;

            // Send email
            if (email != null && !email.trim().isEmpty()) {
                notificationService.sendEmail(email, subject, body);
                emailSent = true;
            }

            // Send SMS
            if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
                notificationService.sendSms(mobileNumber, body);
                smsSent = true;
            }

            // Send push notification
            if (deviceToken != null && !deviceToken.trim().isEmpty()) {
                notificationService.sendPushNotification(deviceToken, subject, body);
                pushSent = true;
            }

            result.put("success", true);
            result.put("email_sent", emailSent);
            result.put("sms_sent", smsSent);
            result.put("push_sent", pushSent);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[send-user-message] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/populate-doc-notification")
    @Operation(summary = "Generate subject and body for document upload notification")
    public ResponseEntity<Map<String, Object>> populateDocNotification(@RequestBody Map<String, Object> variables) {
        log.info("[populate-doc-notification] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object partyTypeObj = variables.get("party_type");
            Object documentTypeObj = variables.get("document_type");

            if (partyTypeObj == null) {
                result.put("success", false);
                result.put("error", "Missing party_type");
                return ResponseEntity.badRequest().body(result);
            }
            if (documentTypeObj == null) {
                result.put("success", false);
                result.put("error", "Missing document_type");
                return ResponseEntity.badRequest().body(result);
            }

            String partyType = partyTypeObj.toString();
            String documentType = documentTypeObj.toString();

            String subject = String.format("New %s uploaded by %s", documentType, partyType);
            String body = String.format(
                    "A %s document has been uploaded by %s to the engagement. You may review it at your earliest convenience.",
                    documentType, partyType
            );

            result.put("success", true);
            result.put("subject", subject);
            result.put("body", body);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[populate-doc-notification] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

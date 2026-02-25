package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.service.NotificationService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * jBPM WorkItemHandler for notification-related workflow tasks.
 * <p>
 * Handles task types:
 * submit-report, notify-admins, send-sms-notification, send-email-notification,
 * send-pushed-notification, send-user-message, populate-doc-notification
 */
public class NotificationWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorkItemHandler.class);

    private final NotificationService notificationService;
    private final String adminEmails;

    public NotificationWorkItemHandler(NotificationService notificationService, String adminEmails) {
        this.notificationService = notificationService;
        this.adminEmails = adminEmails;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        if (taskType == null) taskType = workItem.getName();
        log.info("[NotificationWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            switch (taskType) {
                case "submit-report":
                    handleSubmitReport(workItem, results);
                    break;
                case "notify-admins":
                    handleNotifyAdmins(workItem, results);
                    break;
                case "send-sms-notification":
                    handleSendSms(workItem, results);
                    break;
                case "send-email-notification":
                    handleSendEmail(workItem, results);
                    break;
                case "send-pushed-notification":
                    handleSendPush(workItem, results);
                    break;
                case "send-user-message":
                    handleSendUserMessage(workItem, results);
                    break;
                case "populate-doc-notification":
                    handlePopulateDocNotification(workItem, results);
                    break;
                case "generate-verification-code":
                    handleGenerateVerificationCode(workItem, results);
                    break;
                case "make-report":
                    handleMakeReport(workItem, results);
                    break;
                default:
                    log.warn("[NotificationWorkItemHandler] Unknown taskType: {}", taskType);
                    results.put("success", false);
                    results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[NotificationWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[NotificationWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementations ==========

    private void handleSubmitReport(WorkItem workItem, Map<String, Object> results) {
        String entity = getStringParam(workItem, "entity", "N/A");
        String issue = getStringParam(workItem, "issue", "N/A");
        String personId = getStringParam(workItem, "person_id", "N/A");
        String professionalId = getStringParam(workItem, "professional_id", "N/A");
        String propertyId = getStringParam(workItem, "property_id", "N/A");
        String propertyName = getStringParam(workItem, "property_name", "N/A");
        String reporterId = getStringParam(workItem, "reporter_id", "N/A");
        String reporterName = getStringParam(workItem, "reporter_name", "N/A");

        String body = String.format("""
                A client has submitted a report regarding the following: %s

                Name/Item: %s
                Issue Reported: %s

                Reported By: %s (ID: %s)

                Reference IDs:
                • Property ID: %s
                • Person ID: %s
                • Professional ID: %s

                Please review this report in the admin portal.
                """, entity, propertyName, issue, reporterName, reporterId,
                propertyId, personId, professionalId);

        results.put("body", body);
        results.put("success", true);
    }

    private void handleNotifyAdmins(WorkItem workItem, Map<String, Object> results) {
        String subject = (String) workItem.getParameter("subject");
        String body = (String) workItem.getParameter("body");

        if (adminEmails == null || adminEmails.trim().isEmpty()) {
            log.error("[notify-admins] No admin emails configured");
            results.put("success", false);
            results.put("error", "No admin emails configured");
            return;
        }

        List<String> emailList = Arrays.asList(adminEmails.split(","));
        for (String email : emailList) {
            try {
                notificationService.sendEmail(email.trim(), subject, body);
                log.info("[notify-admins] Email sent to {}", email.trim());
            } catch (Exception e) {
                log.error("[notify-admins] Failed to send email to {}: {}", email.trim(), e.getMessage());
            }
        }

        results.put("success", true);
        results.put("admins_notified", emailList.size());
    }

    private void handleSendSms(WorkItem workItem, Map<String, Object> results) {
        String mobileNumber = (String) workItem.getParameter("mobile_number");
        String message = (String) workItem.getParameter("message");

        if (mobileNumber == null) {
            log.error("[send-sms] No mobile number provided");
            results.put("success", false);
            results.put("error", "Missing mobile_number");
            return;
        }

        notificationService.sendSms(mobileNumber, message);
        results.put("success", true);
    }

    private void handleSendEmail(WorkItem workItem, Map<String, Object> results) {
        String email = (String) workItem.getParameter("email");
        String subject = (String) workItem.getParameter("subject");
        String body = (String) workItem.getParameter("body");

        if (email == null) {
            log.error("[send-email] No email provided");
            results.put("success", false);
            results.put("error", "Missing email");
            return;
        }

        notificationService.sendEmail(email, subject, body);
        results.put("success", true);
    }

    private void handleSendPush(WorkItem workItem, Map<String, Object> results) {
        String deviceToken = (String) workItem.getParameter("device_token");
        String subject = (String) workItem.getParameter("subject");
        String body = (String) workItem.getParameter("body");

        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            log.error("[send-push] No device token provided");
            results.put("success", false);
            results.put("error", "Missing device_token");
            return;
        }

        notificationService.sendPushNotification(deviceToken, subject, body);
        results.put("success", true);
    }

    private void handleSendUserMessage(WorkItem workItem, Map<String, Object> results) {
        String email = (String) workItem.getParameter("email");
        String mobileNumber = (String) workItem.getParameter("mobile_number");
        String deviceToken = (String) workItem.getParameter("device_token");
        String subject = (String) workItem.getParameter("subject");
        String body = (String) workItem.getParameter("body");

        if (email != null) {
            notificationService.sendEmail(email, subject, body);
        }
        if (mobileNumber != null) {
            notificationService.sendSms(mobileNumber, body);
        }
        if (deviceToken != null && !deviceToken.trim().isEmpty()) {
            notificationService.sendPushNotification(deviceToken, subject, body);
        }

        results.put("success", true);
    }

    private void handlePopulateDocNotification(WorkItem workItem, Map<String, Object> results) {
        Object partyTypeObj = workItem.getParameter("party_type");
        Object documentTypeObj = workItem.getParameter("document_type");

        if (partyTypeObj == null || documentTypeObj == null) {
            results.put("success", false);
            results.put("error", "Missing party_type or document_type");
            return;
        }

        String partyType = partyTypeObj.toString();
        String documentType = documentTypeObj.toString();

        String subject = String.format("New %s uploaded by %s", documentType, partyType);
        String body = String.format(
                "A %s document has been uploaded by %s to the engagement. You may review it at your earliest convenience.",
                documentType, partyType);

        results.put("success", true);
        results.put("subject", subject);
        results.put("body", body);
    }

    private void handleGenerateVerificationCode(WorkItem workItem, Map<String, Object> results) {
        String email = getStringParam(workItem, "email", null);
        String mobileNumber = getStringParam(workItem, "mobile_number", null);
        String kycType = getStringParam(workItem, "kyc_type", "EMAIL");

        // Generate a random 6-digit verification code
        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        log.info("[generate-verification-code] Generated code for kycType={}, email={}, mobile={}",
                kycType, email, mobileNumber);

        results.put("confirmation_code", code);
        results.put("email", email);
        results.put("mobile_number", mobileNumber);
        results.put("kyc_type", kycType);
        results.put("success", true);
    }

    private void handleMakeReport(WorkItem workItem, Map<String, Object> results) {
        // Pass through all work item parameters as results so they flow back
        // to process variables (property_id, person_id, issue, etc.)
        for (Map.Entry<String, Object> entry : workItem.getParameters().entrySet()) {
            if (!"TaskType".equals(entry.getKey()) && entry.getValue() != null) {
                results.put(entry.getKey(), entry.getValue());
            }
        }
        log.info("[make-report] Passed through {} parameters", results.size());
        results.put("success", true);
    }

    // ========== Helper ==========

    private String getStringParam(WorkItem workItem, String key, String defaultValue) {
        Object val = workItem.getParameter(key);
        return val != null ? val.toString() : defaultValue;
    }
}

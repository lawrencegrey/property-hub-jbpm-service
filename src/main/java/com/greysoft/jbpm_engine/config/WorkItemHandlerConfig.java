package com.greysoft.jbpm_engine.config;

import com.greysoft.jbpm_engine.service.*;
import com.greysoft.jbpm_engine.workitemhandler.*;
import org.kie.api.runtime.process.WorkItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring configuration that creates and exposes all jBPM WorkItemHandler beans.
 * <p>
 * Each handler is registered under all of the BPMN service task type names it supports.
 * A consolidated {@code Map<String, WorkItemHandler>} bean is also provided so that
 * the KIE session (or any integration layer) can iterate and register them:
 * <pre>
 *   workItemHandlers.forEach((name, handler) ->
 *       ksession.getWorkItemManager().registerWorkItemHandler(name, handler));
 * </pre>
 */
@Configuration
public class WorkItemHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(WorkItemHandlerConfig.class);

    // ===== Handler Beans =====

    @Bean
    public DocumentWorkItemHandler documentWorkItemHandler(DocumentService documentService) {
        log.info("[WorkItemHandlerConfig] Creating DocumentWorkItemHandler");
        return new DocumentWorkItemHandler(documentService);
    }

    @Bean
    public PersonWorkItemHandler personWorkItemHandler(PersonService personService, DocumentService documentService) {
        log.info("[WorkItemHandlerConfig] Creating PersonWorkItemHandler");
        return new PersonWorkItemHandler(personService, documentService);
    }

    @Bean
    public PropertyWorkItemHandler propertyWorkItemHandler(PropertyService propertyService,
                                                           PersonService personService,
                                                           NotificationService notificationService) {
        log.info("[WorkItemHandlerConfig] Creating PropertyWorkItemHandler");
        return new PropertyWorkItemHandler(propertyService, personService, notificationService);
    }

    @Bean
    public NotificationWorkItemHandler notificationWorkItemHandler(
            NotificationService notificationService,
            @Value("${property.platform.admin.emails:}") String adminEmails) {
        log.info("[WorkItemHandlerConfig] Creating NotificationWorkItemHandler (adminEmails={})", adminEmails);
        return new NotificationWorkItemHandler(notificationService, adminEmails);
    }

    @Bean
    public VerificationWorkItemHandler verificationWorkItemHandler(VerificationService verificationService,
                                                                    DocumentService documentService) {
        log.info("[WorkItemHandlerConfig] Creating VerificationWorkItemHandler");
        return new VerificationWorkItemHandler(verificationService, documentService);
    }

    @Bean
    public AgreementWorkItemHandler agreementWorkItemHandler(PropertyService propertyService) {
        log.info("[WorkItemHandlerConfig] Creating AgreementWorkItemHandler");
        return new AgreementWorkItemHandler(propertyService);
    }

    // ===== Registry Bean =====

    /**
     * A name → handler map covering every BPMN service task type.
     * <p>
     * Usage:
     * <pre>
     * &#64;Autowired Map&lt;String, WorkItemHandler&gt; workItemHandlers;
     *
     * // at KIE session creation time:
     * workItemHandlers.forEach((name, handler) -&gt;
     *     ksession.getWorkItemManager().registerWorkItemHandler(name, handler));
     * </pre>
     */
    @Bean("workItemHandlerRegistry")
    public Map<String, WorkItemHandler> workItemHandlerRegistry(
            DocumentWorkItemHandler documentHandler,
            PersonWorkItemHandler personHandler,
            PropertyWorkItemHandler propertyHandler,
            NotificationWorkItemHandler notificationHandler,
            VerificationWorkItemHandler verificationHandler,
            AgreementWorkItemHandler agreementHandler) {

        Map<String, WorkItemHandler> registry = new LinkedHashMap<>();

        // ---- Document tasks ----
        registry.put("update-document-manual-review", documentHandler);
        registry.put("delete-property-documents", documentHandler);
        registry.put("delete-person-documents", documentHandler);
        registry.put("delete-document", documentHandler);

        // ---- Person tasks ----
        registry.put("update-kyc-data", personHandler);
        registry.put("delete-kyc-data", personHandler);
        registry.put("get-person-details", personHandler);
        registry.put("get-profession-details", personHandler);
        registry.put("delete-professional", personHandler);
        registry.put("update-professional", personHandler);
        registry.put("verify-professional", personHandler);
        registry.put("delete-address", personHandler);
        registry.put("verify-address", personHandler);
        registry.put("activate-person", personHandler);
        registry.put("delete-all-person-data", personHandler);

        // ---- Property tasks ----
        registry.put("update-property-data", propertyHandler);
        registry.put("delete-property", propertyHandler);
        registry.put("activate-property", propertyHandler);
        registry.put("offer-property", propertyHandler);
        registry.put("add-offer", propertyHandler);
        registry.put("update-offer", propertyHandler);
        registry.put("update-offer-with-process-instance", propertyHandler);
        registry.put("delete-offer", propertyHandler);
        registry.put("create-engagement", propertyHandler);
        registry.put("add-engagement", propertyHandler);
        registry.put("create-engagement-party", propertyHandler);
        registry.put("add-engagement-party", propertyHandler);
        registry.put("delete-engagement-party", propertyHandler);
        registry.put("update-engagement-party", propertyHandler);
        registry.put("get-offer-participants", propertyHandler);
        registry.put("get-request-participants", propertyHandler);
        registry.put("notify-participants", propertyHandler);

        // ---- Notification tasks ----
        registry.put("submit-report", notificationHandler);
        registry.put("notify-admins", notificationHandler);
        registry.put("send-sms-notification", notificationHandler);
        registry.put("send-email-notification", notificationHandler);
        registry.put("send-pushed-notification", notificationHandler);
        registry.put("send-user-message", notificationHandler);
        registry.put("populate-doc-notification", notificationHandler);
        registry.put("generate-verification-code", notificationHandler);
        registry.put("make-report", notificationHandler);

        // ---- Verification tasks ----
        registry.put("auto-verify-document", verificationHandler);

        // ---- Agreement tasks ----
        registry.put("activate-rental", agreementHandler);
        registry.put("activate-sale", agreementHandler);
        registry.put("activate-lease", agreementHandler);

        log.info("[WorkItemHandlerConfig] Registered {} work item handlers", registry.size());
        return registry;
    }
}

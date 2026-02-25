package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import com.greysoft.jbpm_engine.service.DocumentService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * jBPM WorkItemHandler for document-related workflow tasks.
 * <p>
 * Handles task types via the "TaskType" parameter:
 * <ul>
 *   <li>update-document-manual-review</li>
 *   <li>delete-property-documents</li>
 *   <li>delete-person-documents</li>
 *   <li>delete-document</li>
 * </ul>
 * Register each task type name in BPMN → this handler.
 */
public class DocumentWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentWorkItemHandler.class);

    private final DocumentService documentService;

    public DocumentWorkItemHandler(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        log.info("[DocumentWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            if (taskType == null) taskType = workItem.getName();

            switch (taskType) {
                case "update-document-manual-review":
                    handleUpdateManualReview(workItem, results);
                    break;
                case "delete-property-documents":
                    handleDeleteByProperty(workItem, results);
                    break;
                case "delete-person-documents":
                    handleDeleteByPerson(workItem, results);
                    break;
                case "delete-document":
                    handleDeleteDocument(workItem, results);
                    break;
                default:
                    log.warn("[DocumentWorkItemHandler] Unknown taskType: {}", taskType);
                    results.put("success", false);
                    results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[DocumentWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[DocumentWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementations ==========

    private void handleUpdateManualReview(WorkItem workItem, Map<String, Object> results) {
        String documentIdStr = (String) workItem.getParameter("document_id");
        String verificationReason = (String) workItem.getParameter("verification_reason");
        Boolean isVerified = (Boolean) workItem.getParameter("is_verified");

        UUID documentId = UUID.fromString(documentIdStr);

        DocumentDto doc = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Update verification fields
        if (isVerified != null && isVerified) {
            doc.setVerificationStatus("VERIFIED");
        } else {
            doc.setVerificationStatus("REJECTED");
        }

        documentService.updateDocument(documentId, doc);

        results.put("success", true);
        results.put("document_id", documentId.toString());
        log.info("[update-document-manual-review] Updated document {}, verified={}", documentId, isVerified);
    }

    private void handleDeleteByProperty(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        Integer deletedCount = documentService.deleteDocumentsByPropertyId(propertyId);

        results.put("success", true);
        results.put("deleted_count", deletedCount);
        results.put("property_id", propertyId.toString());
        log.info("[delete-property-documents] Deleted {} documents for propertyId={}", deletedCount, propertyId);
    }

    private void handleDeleteByPerson(WorkItem workItem, Map<String, Object> results) {
        Object personIdObj = workItem.getParameter("person_id");
        if (personIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_id");
            return;
        }

        UUID personId = UUID.fromString(personIdObj.toString());
        Integer deletedCount = documentService.deleteDocumentsByPersonId(personId);

        results.put("success", true);
        results.put("deleted_count", deletedCount);
        results.put("person_id", personId.toString());
        log.info("[delete-person-documents] Deleted {} documents for personId={}", deletedCount, personId);
    }

    private void handleDeleteDocument(WorkItem workItem, Map<String, Object> results) {
        Object documentIdObj = workItem.getParameter("document_id");
        if (documentIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing document_id");
            return;
        }

        UUID documentId = UUID.fromString(documentIdObj.toString());
        boolean deleted = documentService.deleteDocument(documentId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("document_id", documentId.toString());
        if (!deleted) results.put("error", "Document not found");
        log.info("[delete-document] Document {} deleted={}", documentId, deleted);
    }
}

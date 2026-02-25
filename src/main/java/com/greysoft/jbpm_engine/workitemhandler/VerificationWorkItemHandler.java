package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import com.greysoft.jbpm_engine.service.DocumentService;
import com.greysoft.jbpm_engine.service.VerificationService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * jBPM WorkItemHandler for document verification tasks.
 * <p>
 * Handles task type: auto-verify-document
 * <p>
 * Dispatches to the appropriate verification method based on the document type
 * (PASSPORT, ID_CARD, PROOF_OF_ADDRESS, PROFESSIONAL_LICENSE, TAX_CARD).
 */
public class VerificationWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(VerificationWorkItemHandler.class);

    private final VerificationService verificationService;
    private final DocumentService documentService;

    public VerificationWorkItemHandler(VerificationService verificationService, DocumentService documentService) {
        this.verificationService = verificationService;
        this.documentService = documentService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        if (taskType == null) taskType = workItem.getName();
        log.info("[VerificationWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            if ("auto-verify-document".equals(taskType)) {
                handleAutoVerifyDocument(workItem, results);
            } else {
                log.warn("[VerificationWorkItemHandler] Unknown taskType: {}", taskType);
                results.put("success", false);
                results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[VerificationWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[VerificationWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementation ==========

    private void handleAutoVerifyDocument(WorkItem workItem, Map<String, Object> results) {
        String documentIdStr = (String) workItem.getParameter("document_id");
        if (documentIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing document_id");
            return;
        }

        UUID documentId = UUID.fromString(documentIdStr);
        log.info("[auto-verify-document] Starting verification for documentId={}", documentId);

        // Get document details
        DocumentDto documentDetails = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // Get the file bytes
        byte[] fileBytes = documentService.getFile(documentId);
        if (fileBytes == null || fileBytes.length == 0) {
            results.put("success", false);
            results.put("error", "Failed to retrieve file for document: " + documentId);
            return;
        }

        String documentType = documentDetails.getDocumentType();
        Object verificationResult = null;
        boolean isAutoVerified = false;

        switch (documentType != null ? documentType : "") {
            case "PASSPORT":
                log.info("[auto-verify-document] Verifying passport for documentId={}", documentId);
                Map<String, Object> passportResult = verificationService.verifyPassport(fileBytes, documentDetails.getFileName());
                verificationResult = passportResult;
                isAutoVerified = passportResult != null
                        && Boolean.TRUE.equals(passportResult.get("verified"));
                log.info("[auto-verify-document] Passport result: isAutoVerified={}", isAutoVerified);
                break;

            case "ID_CARD":
                log.info("[auto-verify-document] Verifying government ID for documentId={}", documentId);
                // For ID_CARD, we also need the passport pic for face comparison
                String passportPicDocId = (String) workItem.getParameter("passport_pic_doc_id");
                if (passportPicDocId != null) {
                    UUID passportPicUuid = UUID.fromString(passportPicDocId);
                    byte[] passportFileBytes = documentService.getFile(passportPicUuid);
                    if (passportFileBytes != null && passportFileBytes.length > 0) {
                        Map<String, Object> govIdResult = verificationService.verifyGovernmentId(
                                passportFileBytes, documentDetails.getFileName());
                        verificationResult = govIdResult;
                        isAutoVerified = govIdResult != null
                                && Boolean.TRUE.equals(govIdResult.get("verified"));
                    }
                } else {
                    // Fallback: verify with just the ID document
                    Map<String, Object> govIdResult = verificationService.verifyGovernmentId(
                            fileBytes, documentDetails.getFileName());
                    verificationResult = govIdResult;
                    isAutoVerified = govIdResult != null
                            && Boolean.TRUE.equals(govIdResult.get("verified"));
                }
                log.info("[auto-verify-document] Government ID result: isAutoVerified={}", isAutoVerified);
                break;

            case "PROOF_OF_ADDRESS":
                log.info("[auto-verify-document] Verifying proof of address for documentId={}", documentId);
                Map<String, Object> poaResult = verificationService.verifyProofOfAddress(
                        fileBytes, documentDetails.getFileName());
                verificationResult = poaResult;
                isAutoVerified = poaResult != null
                        && Boolean.TRUE.equals(poaResult.get("verified"));
                log.info("[auto-verify-document] Proof of address result: isAutoVerified={}", isAutoVerified);
                break;

            case "PROFESSIONAL_LICENSE":
                log.info("[auto-verify-document] Professional license verification for documentId={}", documentId);
                // jBPM VerificationService doesn't have a specific verifyProfessionalLicense method,
                // so we use proof-of-address verification as a general document check
                Map<String, Object> licenseResult = verificationService.verifyProofOfAddress(
                        fileBytes, documentDetails.getFileName());
                verificationResult = licenseResult;
                isAutoVerified = licenseResult != null
                        && Boolean.TRUE.equals(licenseResult.get("verified"));
                log.info("[auto-verify-document] Professional license result: isAutoVerified={}", isAutoVerified);
                break;

            case "TAX_CARD":
                log.info("[auto-verify-document] Verifying tax card for documentId={}", documentId);
                // Use passport verification as a general document check for tax card
                Map<String, Object> taxResult = verificationService.verifyPassport(
                        fileBytes, documentDetails.getFileName());
                verificationResult = taxResult;
                isAutoVerified = taxResult != null
                        && Boolean.TRUE.equals(taxResult.get("verified"));
                log.info("[auto-verify-document] Tax card result: isAutoVerified={}", isAutoVerified);
                break;

            default:
                log.warn("[auto-verify-document] Unknown document type: {} for documentId={}", documentType, documentId);
                results.put("success", false);
                results.put("error", "Unknown document type: " + documentType);
                return;
        }

        // Update document verification status
        documentService.updateDocumentVerification(documentId, "auto", verificationResult, isAutoVerified);

        results.put("success", true);
        results.put("document_id", documentId.toString());
        results.put("document_type", documentType);
        results.put("is_auto_verified", isAutoVerified);
        results.put("verification_method", "auto");
    }
}

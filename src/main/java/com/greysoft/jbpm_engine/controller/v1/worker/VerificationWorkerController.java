package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import com.greysoft.jbpm_engine.service.DocumentService;
import com.greysoft.jbpm_engine.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST endpoints for verification worker operations.
 * Called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker method: auto-verify-document
 */
@RestController
@RequestMapping("/api/v1/workers/verification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification Worker", description = "REST endpoints for verification workflow operations (called by jBPM)")
public class VerificationWorkerController {

    private final VerificationService verificationService;
    private final DocumentService documentService;

    @PostMapping("/auto-verify-document")
    @Operation(summary = "Auto-verify a document based on its type")
    public ResponseEntity<Map<String, Object>> autoVerifyDocument(@RequestBody Map<String, Object> variables) {
        log.info("[auto-verify-document] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String documentId = (String) variables.get("document_id");
            if (documentId == null || documentId.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing document_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID documentUuid = UUID.fromString(documentId);

            // Get document details
            Optional<DocumentDto> docOpt = documentService.getDocumentById(documentUuid);
            if (docOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Document not found: " + documentId);
                return ResponseEntity.ok(result);
            }

            DocumentDto document = docOpt.get();
            String documentType = document.getDocumentType();

            // Download the file
            byte[] fileBytes = documentService.getFile(documentUuid);
            if (fileBytes == null || fileBytes.length == 0) {
                result.put("success", false);
                result.put("error", "Could not download document file");
                return ResponseEntity.ok(result);
            }

            Map<String, Object> verificationResult = null;
            boolean isAutoVerified = false;

            switch (documentType != null ? documentType.toUpperCase() : "") {
                case "PASSPORT":
                    log.info("Verifying passport for documentId={}, fileName={}", documentUuid, document.getFileName());
                    verificationResult = verificationService.verifyPassport(fileBytes, document.getFileName());
                    isAutoVerified = isVerified(verificationResult);
                    break;

                case "ID_CARD":
                    log.info("Verifying government ID for documentId={}", documentUuid);
                    // For ID_CARD, we also need the passport pic to compare
                    String passportPicDocId = (String) variables.get("passport_pic_doc_id");
                    if (passportPicDocId != null && !passportPicDocId.trim().isEmpty()) {
                        UUID passportPicUuid = UUID.fromString(passportPicDocId);
                        Optional<DocumentDto> passportPicDoc = documentService.getDocumentById(passportPicUuid);
                        byte[] passportFileBytes = documentService.getFile(passportPicUuid);
                        if (passportPicDoc.isPresent() && passportFileBytes != null) {
                            verificationResult = verificationService.verifyGovernmentId(
                                    passportFileBytes, passportPicDoc.get().getFileName());
                        }
                    } else {
                        verificationResult = verificationService.verifyGovernmentId(fileBytes, document.getFileName());
                    }
                    isAutoVerified = isVerified(verificationResult);
                    break;

                case "PROOF_OF_ADDRESS":
                    log.info("Verifying proof of address for documentId={}", documentUuid);
                    verificationResult = verificationService.verifyProofOfAddress(fileBytes, document.getFileName());
                    isAutoVerified = isVerified(verificationResult);
                    break;

                case "PROFESSIONAL_LICENSE":
                    log.info("Verifying professional license for documentId={}", documentUuid);
                    // Use proof-of-address endpoint as generic doc verification (closest match)
                    verificationResult = verificationService.verifyProofOfAddress(fileBytes, document.getFileName());
                    isAutoVerified = isVerified(verificationResult);
                    break;

                case "TAX_CARD":
                    log.info("Verifying tax card for documentId={}", documentUuid);
                    verificationResult = verificationService.verifyProofOfAddress(fileBytes, document.getFileName());
                    isAutoVerified = isVerified(verificationResult);
                    break;

                default:
                    log.warn("Unknown document type: {} for documentId={}", documentType, documentUuid);
                    result.put("success", false);
                    result.put("error", "Unknown document type: " + documentType);
                    return ResponseEntity.ok(result);
            }

            // Update document verification status
            documentService.updateDocumentVerification(documentUuid, "auto", verificationResult, isAutoVerified);

            result.put("success", true);
            result.put("document_id", documentId);
            result.put("document_type", documentType);
            result.put("is_auto_verified", isAutoVerified);
            result.put("verification_result", verificationResult);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[auto-verify-document] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Helper to check if a verification result indicates a successful verification.
     */
    @SuppressWarnings("unchecked")
    private boolean isVerified(Map<String, Object> verificationResult) {
        if (verificationResult == null) return false;
        try {
            Object resultObj = verificationResult.get("result");
            if (resultObj instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                Object verified = resultMap.get("verified");
                if (verified instanceof Boolean) return (Boolean) verified;
                if (verified != null) return Boolean.parseBoolean(verified.toString());
            }
            // Also check top-level
            Object verified = verificationResult.get("verified");
            if (verified instanceof Boolean) return (Boolean) verified;
            if (verified != null) return Boolean.parseBoolean(verified.toString());
        } catch (Exception e) {
            log.warn("Error parsing verification result: {}", e.getMessage());
        }
        return false;
    }
}

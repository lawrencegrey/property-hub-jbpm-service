package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import com.greysoft.jbpm_engine.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST endpoints for document worker operations.
 * These are called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker methods:
 *  - update-document-manual-review
 *  - delete-property-documents
 *  - delete-person-documents
 *  - delete-document
 */
@RestController
@RequestMapping("/api/v1/workers/document")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Worker", description = "REST endpoints for document workflow operations (called by jBPM)")
public class DocumentWorkerController {

    private final DocumentService documentService;

    /**
     * POST /api/v1/workers/document/update-manual-review
     * Input: { document_id, verification_reason, is_verified }
     */
    @PostMapping("/update-manual-review")
    @Operation(summary = "Update document after manual review", description = "Sets manual verification result on a document")
    public ResponseEntity<Map<String, Object>> updateDocumentManualReview(@RequestBody Map<String, Object> variables) {
        log.info("[update-document-manual-review] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            UUID documentId = UUID.fromString((String) variables.get("document_id"));
            String verificationReason = (String) variables.get("verification_reason");
            Boolean isVerified = (Boolean) variables.get("is_verified");

            DocumentDto documentDetails = documentService.getDocumentById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

            // Update verification fields via the document service
            documentService.updateDocumentVerification(documentId, "manual", 
                    Map.of("reason", verificationReason != null ? verificationReason : "",
                           "is_verified", isVerified != null ? isVerified : false),
                    isVerified != null && isVerified);

            result.put("success", true);
            result.put("document_id", documentId.toString());
            result.put("is_verified", isVerified);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("[update-document-manual-review] Error: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("[update-document-manual-review] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * POST /api/v1/workers/document/delete-by-property
     * Input: { property_id }
     */
    @PostMapping("/delete-by-property")
    @Operation(summary = "Delete all documents for a property")
    public ResponseEntity<Map<String, Object>> deleteDocumentsByProperty(@RequestBody Map<String, Object> variables) {
        log.info("[delete-property-documents] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID propertyId = UUID.fromString(propertyIdObj.toString());
            Integer deletedCount = documentService.deleteDocumentsByPropertyId(propertyId);

            result.put("success", true);
            result.put("deleted_count", deletedCount);
            result.put("property_id", propertyId.toString());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", "Invalid property ID format");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * POST /api/v1/workers/document/delete-by-person
     * Input: { person_id }
     */
    @PostMapping("/delete-by-person")
    @Operation(summary = "Delete all documents for a person")
    public ResponseEntity<Map<String, Object>> deleteDocumentsByPerson(@RequestBody Map<String, Object> variables) {
        log.info("[delete-person-documents] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personIdObj = variables.get("person_id");
            if (personIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdObj.toString());
            Integer deletedCount = documentService.deleteDocumentsByPersonId(personId);

            result.put("success", true);
            result.put("deleted_count", deletedCount);
            result.put("person_id", personId.toString());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", "Invalid person ID format");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * POST /api/v1/workers/document/delete
     * Input: { document_id }
     */
    @PostMapping("/delete")
    @Operation(summary = "Delete a single document")
    public ResponseEntity<Map<String, Object>> deleteDocument(@RequestBody Map<String, Object> variables) {
        log.info("[delete-document] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object documentIdObj = variables.get("document_id");
            if (documentIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing document_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID documentId = UUID.fromString(documentIdObj.toString());
            boolean deleted = documentService.deleteDocument(documentId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("document_id", documentId.toString());
            if (!deleted) {
                result.put("error", "Document not found");
            }
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", "Invalid document ID format");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

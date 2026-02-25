package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.dto.KycDto;
import com.greysoft.jbpm_engine.dto.people.AddressDto;
import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.people.PersonRoleDto;
import com.greysoft.jbpm_engine.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST endpoints for person worker operations.
 * Called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker methods:
 *  - update-kyc-data, delete-kyc-data
 *  - get-profession-details, get-person-details
 *  - delete-address, delete-professional, update-professional
 *  - verify-professional, verify-address
 *  - activate-person, delete-all-person-data
 */
@RestController
@RequestMapping("/api/v1/workers/person")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Person Worker", description = "REST endpoints for person workflow operations (called by jBPM)")
public class PersonWorkerController {

    private final PersonService personService;

    // ==================== KYC Operations ====================

    @PostMapping("/update-kyc-data")
    @Operation(summary = "Update KYC data after document verification")
    public ResponseEntity<Map<String, Object>> updateKycData(@RequestBody Map<String, Object> variables) {
        log.info("[update-kyc-data] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String personIdStr = (String) variables.get("person_id");
            String documentId = (String) variables.get("document_id");
            String documentType = (String) variables.get("document_type");

            if (personIdStr == null || personIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdStr.trim());
            KycDto kycDto = personService.getKycByPersonIdAsDto(personId)
                    .orElseThrow(() -> new IllegalArgumentException("KYC data not found for person: " + personId));

            KycDto updatedKyc = updateKycDataHelper(kycDto, documentType, documentId);
            personService.updateKyc(kycDto.getId(), updatedKyc).block();

            result.put("success", true);
            result.put("person_id", personId.toString());
            result.put("document_type", documentType);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-kyc-data] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/delete-kyc-data")
    @Operation(summary = "Delete/reset KYC data for a document type")
    public ResponseEntity<Map<String, Object>> deleteKycData(@RequestBody Map<String, Object> variables) {
        log.info("[delete-kyc-data] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String personIdStr = (String) variables.get("person_id");
            String documentId = (String) variables.get("document_id");
            String documentType = (String) variables.get("document_type");

            if (personIdStr == null || personIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdStr.trim());
            KycDto kycDto = personService.getKycByPersonIdAsDto(personId)
                    .orElseThrow(() -> new IllegalArgumentException("KYC data not found for person: " + personId));

            KycDto updatedKyc = deleteKycDataHelper(kycDto, documentType, documentId);
            personService.updateKyc(kycDto.getId(), updatedKyc).block();

            result.put("success", true);
            result.put("person_id", personId.toString());
            result.put("document_type", documentType);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-kyc-data] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Person Details ====================

    @PostMapping("/get-person-details")
    @Operation(summary = "Get person details by ID")
    public ResponseEntity<Map<String, Object>> getPersonDetails(@RequestBody Map<String, Object> variables) {
        log.info("[get-person-details] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personIdObj = variables.get("person_id");
            if (personIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdObj.toString());
            Optional<PersonDto> personOpt = personService.getPersonById(personId);

            if (personOpt.isPresent()) {
                PersonDto person = personOpt.get();
                result.put("success", true);
                result.put("person_id", personId.toString());
                result.put("first_name", person.getFirstName());
                result.put("last_name", person.getLastName());
                result.put("email", person.getEmail());
                result.put("mobile_number", person.getMobileNumber());
                result.put("device_token", person.getDeviceToken());
            } else {
                result.put("success", false);
                result.put("error", "Person not found");
                result.put("person_id", personId.toString());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[get-person-details] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Profession / Role Operations ====================

    @PostMapping("/get-profession-details")
    @Operation(summary = "Get profession (person role) details")
    public ResponseEntity<Map<String, Object>> getProfessionDetails(@RequestBody Map<String, Object> variables) {
        log.info("[get-profession-details] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personRoleIdObj = variables.get("person_role_id");
            if (personRoleIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_role_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personRoleId = UUID.fromString(personRoleIdObj.toString());

            return personService.getPersonRoleById(personRoleId)
                    .map(roleDetails -> {
                        result.put("success", true);
                        result.put("person_role_id", personRoleId.toString());
                        result.put("document_id", roleDetails.getDocumentId() != null ? roleDetails.getDocumentId().toString() : null);
                        result.put("address_id", roleDetails.getAddressId() != null ? roleDetails.getAddressId().toString() : null);

                        if (roleDetails.getAddressId() != null) {
                            personService.getAddressById(roleDetails.getAddressId())
                                    .ifPresent(address -> result.put("address_document_id",
                                            address.getDocumentId() != null ? address.getDocumentId().toString() : null));
                        }
                        return ResponseEntity.ok(result);
                    })
                    .orElseGet(() -> {
                        result.put("success", false);
                        result.put("error", "Person role not found");
                        result.put("person_role_id", personRoleId.toString());
                        return ResponseEntity.ok(result);
                    });

        } catch (Exception e) {
            log.error("[get-profession-details] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/delete-professional")
    @Operation(summary = "Delete a professional (person role)")
    public ResponseEntity<Map<String, Object>> deleteProfessional(@RequestBody Map<String, Object> variables) {
        log.info("[delete-professional] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personRoleIdObj = variables.get("person_role_id");
            if (personRoleIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_role_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
            boolean deleted = personService.deletePersonRole(personRoleId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("person_role_id", personRoleId.toString());
            if (!deleted) result.put("error", "Professional not found");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-professional] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/update-professional")
    @Operation(summary = "Update professional - set verified and active")
    public ResponseEntity<Map<String, Object>> updateProfessional(@RequestBody Map<String, Object> variables) {
        log.info("[update-professional] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personRoleIdObj = variables.get("person_role_id");
            if (personRoleIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_role_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
            Optional<PersonRoleDto> currentRole = personService.getPersonRoleById(personRoleId);
            if (currentRole.isEmpty()) {
                result.put("success", false);
                result.put("error", "Person role not found");
                return ResponseEntity.ok(result);
            }

            PersonRoleDto personRole = currentRole.get();
            personRole.setVerified(true);
            personRole.setActive(true);

            Optional<PersonRoleDto> updated = personService.updatePersonRole(personRoleId, personRole);
            if (updated.isPresent()) {
                result.put("success", true);
                result.put("updated", true);
                result.put("person_role_id", personRoleId.toString());
                result.put("verified", true);
                result.put("active", true);
            } else {
                result.put("success", false);
                result.put("error", "Update failed");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-professional] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/verify-professional")
    @Operation(summary = "Verify a professional role")
    public ResponseEntity<Map<String, Object>> verifyProfessional(@RequestBody Map<String, Object> variables) {
        log.info("[verify-professional] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object personRoleIdObj = variables.get("person_role_id");
            if (personRoleIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing person_role_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
            Optional<PersonRoleDto> verified = personService.verifyPersonRole(personRoleId);

            if (verified.isPresent()) {
                result.put("success", true);
                result.put("verified", true);
                result.put("person_role_id", personRoleId.toString());
                result.put("active", verified.get().isActive());
            } else {
                result.put("success", false);
                result.put("error", "Verification failed - person role not found");
                result.put("person_role_id", personRoleId.toString());
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[verify-professional] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Address Operations ====================

    @PostMapping("/delete-address")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Map<String, Object>> deleteAddress(@RequestBody Map<String, Object> variables) {
        log.info("[delete-address] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object addressIdObj = variables.get("address_id");
            if (addressIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing address_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID addressId = UUID.fromString(addressIdObj.toString());
            boolean deleted = personService.deleteAddress(addressId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("address_id", addressId.toString());
            if (!deleted) result.put("error", "Address not found");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-address] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/verify-address")
    @Operation(summary = "Verify an address (by address_id or document_id)")
    public ResponseEntity<Map<String, Object>> verifyAddress(@RequestBody Map<String, Object> variables) {
        log.info("[verify-address] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object addressIdObj = variables.get("address_id");
            UUID addressId;

            if (addressIdObj == null) {
                Object documentIdObj = variables.get("document_id");
                if (documentIdObj == null) {
                    result.put("success", false);
                    result.put("error", "Missing address_id and document_id");
                    return ResponseEntity.badRequest().body(result);
                }

                UUID documentId = UUID.fromString(documentIdObj.toString());
                Optional<AddressDto> addressOpt = personService.getAddressByDocumentId(documentId);
                if (addressOpt.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "Address not found for document_id");
                    result.put("document_id", documentId.toString());
                    return ResponseEntity.ok(result);
                }
                addressId = addressOpt.get().getId();
            } else {
                addressId = UUID.fromString(addressIdObj.toString());
            }

            boolean verified = personService.verifyAddress(addressId);
            result.put("success", verified);
            result.put("verified", verified);
            result.put("address_id", addressId.toString());
            if (!verified) result.put("error", "Address not found");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[verify-address] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Person Lifecycle ====================

    @PostMapping("/activate-person")
    @Operation(summary = "Activate a person (set active=true, verified=true)")
    public ResponseEntity<Map<String, Object>> activatePerson(@RequestBody Map<String, Object> variables) {
        log.info("[activate-person] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String personIdStr = (String) variables.get("person_id");
            if (personIdStr == null) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdStr);
            Optional<PersonDto> existingOpt = personService.getPersonById(personId);
            if (existingOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Person not found");
                return ResponseEntity.ok(result);
            }

            PersonDto person = existingOpt.get();
            person.setActive(true);
            person.setVerified(true);

            Optional<PersonDto> updated = personService.updatePerson(personId, person);
            if (updated.isPresent()) {
                result.put("success", true);
                result.put("personId", personId.toString());
                result.put("message", "Person updated successfully - active set to true");
            } else {
                result.put("success", false);
                result.put("error", "Failed to update person");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[activate-person] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/delete-all-person-data")
    @Operation(summary = "Delete all data for a person")
    public ResponseEntity<Map<String, Object>> deleteAllPersonData(@RequestBody Map<String, Object> variables) {
        log.info("[delete-all-person-data] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String personIdStr = (String) variables.get("person_id");
            if (personIdStr == null) {
                result.put("success", false);
                result.put("error", "Missing person_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID personId = UUID.fromString(personIdStr);
            personService.deleteAllPersonData(personId);

            result.put("success", true);
            result.put("personId", personId.toString());
            result.put("message", "All person data deleted successfully");
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("[delete-all-person-data] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== KYC Helpers ====================

    private KycDto updateKycDataHelper(KycDto kycDto, String kycType, String documentId) {
        LocalDateTime now = LocalDateTime.now();
        if (kycType == null) kycType = "";

        switch (kycType.toUpperCase()) {
            case "PASSPORT_PIC":
                kycDto.setPassportPicVerified(true);
                kycDto.setPassportPicVerificationDate(now);
                break;
            case "PROOF_OF_ADDRESS":
                kycDto.setAddressVerified(true);
                kycDto.setAddressVerificationDate(now);
                break;
            case "TAX_CARD":
                kycDto.setTrnVerified(true);
                kycDto.setTrnVerificationDate(now);
                break;
            case "LIVELINESS":
                kycDto.setLivelinessVerified(true);
                kycDto.setLivelinessVerificationDate(now);
                break;
            default:
                kycDto.setIdVerified(true);
                kycDto.setIdVerificationDate(now);
        }
        return kycDto;
    }

    private KycDto deleteKycDataHelper(KycDto kycDto, String kycType, String documentId) {
        if (kycType == null) kycType = "";

        switch (kycType.toUpperCase()) {
            case "PASSPORT_PIC":
                kycDto.setPassportPicVerified(false);
                kycDto.setPassportPicVerificationDate(null);
                break;
            case "PROOF_OF_ADDRESS":
                kycDto.setAddressVerified(false);
                kycDto.setAddressVerificationDate(null);
                break;
            case "TAX_CARD":
                kycDto.setTrnVerified(false);
                kycDto.setTrnVerificationDate(null);
                break;
            case "LIVELINESS":
                kycDto.setLivelinessVerified(false);
                kycDto.setLivelinessVerificationDate(null);
                break;
            default:
                kycDto.setIdVerified(false);
                kycDto.setIdVerificationDate(null);
        }
        return kycDto;
    }
}

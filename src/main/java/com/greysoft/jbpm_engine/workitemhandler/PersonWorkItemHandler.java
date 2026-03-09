package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.dto.KycDto;
import com.greysoft.jbpm_engine.dto.people.AddressDto;
import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.people.PersonRoleDto;
import com.greysoft.jbpm_engine.service.DocumentService;
import com.greysoft.jbpm_engine.service.PersonService;
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
 * jBPM WorkItemHandler for person-related workflow tasks.
 * <p>
 * Handles task types (via "TaskType" parameter or BPMN service task name):
 * <ul>
 *   <li>update-kyc-data</li>
 *   <li>delete-kyc-data</li>
 *   <li>get-person-details</li>
 *   <li>get-profession-details</li>
 *   <li>delete-professional</li>
 *   <li>update-professional</li>
 *   <li>verify-professional</li>
 *   <li>delete-address</li>
 *   <li>verify-address</li>
 *   <li>activate-person</li>
 *   <li>delete-all-person-data</li>
 *   <li>update-person-reported</li>
 * </ul>
 */
public class PersonWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(PersonWorkItemHandler.class);

    private final PersonService personService;
    private final DocumentService documentService;

    public PersonWorkItemHandler(PersonService personService, DocumentService documentService) {
        this.personService = personService;
        this.documentService = documentService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        if (taskType == null) taskType = workItem.getName();
        log.info("[PersonWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            switch (taskType) {
                case "update-kyc-data":
                    handleUpdateKycData(workItem, results);
                    break;
                case "delete-kyc-data":
                    handleDeleteKycData(workItem, results);
                    break;
                case "get-person-details":
                    handleGetPersonDetails(workItem, results);
                    break;
                case "get-profession-details":
                    handleGetProfessionDetails(workItem, results);
                    break;
                case "delete-professional":
                    handleDeleteProfessional(workItem, results);
                    break;
                case "update-professional":
                    handleUpdateProfessional(workItem, results);
                    break;
                case "verify-professional":
                    handleVerifyProfessional(workItem, results);
                    break;
                case "delete-address":
                    handleDeleteAddress(workItem, results);
                    break;
                case "verify-address":
                    handleVerifyAddress(workItem, results);
                    break;
                case "activate-person":
                    handleActivatePerson(workItem, results);
                    break;
                case "delete-all-person-data":
                    handleDeleteAllPersonData(workItem, results);
                    break;
                case "update-person-reported":
                    handleUpdatePersonReported(workItem, results);
                    break;
                default:
                    log.warn("[PersonWorkItemHandler] Unknown taskType: {}", taskType);
                    results.put("success", false);
                    results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[PersonWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[PersonWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementations ==========

    private void handleUpdateKycData(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        String documentId = (String) workItem.getParameter("document_id");
        String documentType = (String) workItem.getParameter("document_type");

        UUID personId = UUID.fromString(personIdStr.trim());

        KycDto kycDto = personService.getKycByPersonIdAsDto(personId)
                .orElseThrow(() -> new IllegalArgumentException("KYC not found for personId: " + personId));

        updateKycDataHelper(kycDto, documentType, documentId);

        personService.updateKyc(kycDto.getId(), kycDto)
                .subscribe(
                        updated -> log.info("[update-kyc-data] KYC updated for personId={}", personId),
                        error -> log.error("[update-kyc-data] Error: {}", error.getMessage()));

        results.put("success", true);
        results.put("person_id", personId.toString());
    }

    private void handleDeleteKycData(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        String documentId = (String) workItem.getParameter("document_id");
        String documentType = (String) workItem.getParameter("document_type");

        UUID personId = UUID.fromString(personIdStr.trim());

        KycDto kycDto = personService.getKycByPersonIdAsDto(personId)
                .orElseThrow(() -> new IllegalArgumentException("KYC not found for personId: " + personId));

        deleteKycDataHelper(kycDto, documentType, documentId);

        personService.updateKyc(kycDto.getId(), kycDto)
                .subscribe(
                        updated -> log.info("[delete-kyc-data] KYC cleared for personId={}", personId),
                        error -> log.error("[delete-kyc-data] Error: {}", error.getMessage()));

        results.put("success", true);
        results.put("person_id", personId.toString());
    }

    private void handleGetPersonDetails(WorkItem workItem, Map<String, Object> results) {
        Object personIdObj = workItem.getParameter("person_id");
        if (personIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_id");
            return;
        }

        UUID personId = UUID.fromString(personIdObj.toString());
        Optional<PersonDto> personOpt = personService.getPersonById(personId);

        if (personOpt.isPresent()) {
            PersonDto person = personOpt.get();
            results.put("success", true);
            results.put("person_id", personId.toString());
            results.put("first_name", person.getFirstName());
            results.put("last_name", person.getLastName());
            results.put("email", person.getEmail());
            results.put("mobile_number", person.getMobileNumber());
            results.put("device_token", person.getDeviceToken());
        } else {
            results.put("success", false);
            results.put("error", "Person not found");
        }
    }

    private void handleGetProfessionDetails(WorkItem workItem, Map<String, Object> results) {
        Object personRoleIdObj = workItem.getParameter("person_role_id");
        if (personRoleIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_role_id");
            return;
        }

        UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
        Optional<PersonRoleDto> roleOpt = personService.getPersonRoleById(personRoleId);

        if (roleOpt.isPresent()) {
            PersonRoleDto role = roleOpt.get();
            results.put("success", true);
            results.put("person_role_id", personRoleId.toString());
            results.put("document_id", role.getDocumentId());
            results.put("address_id", role.getAddressId());

            // Also get address document ID if available
            personService.getAddressById(role.getAddressId())
                    .ifPresent(address -> results.put("address_document_id", address.getDocumentId()));
        } else {
            results.put("success", false);
            results.put("error", "Person role not found");
        }
    }

    private void handleDeleteProfessional(WorkItem workItem, Map<String, Object> results) {
        Object personRoleIdObj = workItem.getParameter("person_role_id");
        if (personRoleIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_role_id");
            return;
        }

        UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
        boolean deleted = personService.deletePersonRole(personRoleId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("person_role_id", personRoleId.toString());
        if (!deleted) results.put("error", "Professional not found");
    }

    private void handleUpdateProfessional(WorkItem workItem, Map<String, Object> results) {
        Object personRoleIdObj = workItem.getParameter("person_role_id");
        if (personRoleIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_role_id");
            return;
        }

        UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
        Optional<PersonRoleDto> currentRole = personService.getPersonRoleById(personRoleId);

        if (currentRole.isEmpty()) {
            results.put("success", false);
            results.put("error", "Person role not found");
            return;
        }

        PersonRoleDto role = currentRole.get();
        role.setVerified(true);
        role.setActive(true);

        Optional<PersonRoleDto> updated = personService.updatePersonRole(personRoleId, role);
        results.put("success", updated.isPresent());
        results.put("updated", updated.isPresent());
        results.put("person_role_id", personRoleId.toString());
    }

    private void handleVerifyProfessional(WorkItem workItem, Map<String, Object> results) {
        Object personRoleIdObj = workItem.getParameter("person_role_id");
        if (personRoleIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing person_role_id");
            return;
        }

        UUID personRoleId = UUID.fromString(personRoleIdObj.toString());
        Optional<PersonRoleDto> verified = personService.verifyPersonRole(personRoleId);

        results.put("success", verified.isPresent());
        results.put("verified", verified.isPresent());
        results.put("person_role_id", personRoleId.toString());
        if (verified.isPresent()) {
            results.put("active", verified.get().isActive());
        }
    }

    private void handleDeleteAddress(WorkItem workItem, Map<String, Object> results) {
        Object addressIdObj = workItem.getParameter("address_id");
        if (addressIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing address_id");
            return;
        }

        UUID addressId = UUID.fromString(addressIdObj.toString());
        boolean deleted = personService.deleteAddress(addressId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("address_id", addressId.toString());
        if (!deleted) results.put("error", "Address not found");
    }

    private void handleVerifyAddress(WorkItem workItem, Map<String, Object> results) {
        Object addressIdObj = workItem.getParameter("address_id");
        UUID addressId;

        if (addressIdObj == null) {
            // Fallback: look up address by document_id
            Object documentIdObj = workItem.getParameter("document_id");
            if (documentIdObj == null) {
                results.put("success", false);
                results.put("error", "Missing address_id and document_id");
                return;
            }

            UUID documentId = UUID.fromString(documentIdObj.toString());
            Optional<AddressDto> addressOpt = personService.getAddressByDocumentId(documentId);
            if (addressOpt.isEmpty()) {
                results.put("success", false);
                results.put("error", "Address not found for document_id");
                return;
            }
            addressId = addressOpt.get().getId();
        } else {
            addressId = UUID.fromString(addressIdObj.toString());
        }

        boolean verified = personService.verifyAddress(addressId);
        results.put("success", verified);
        results.put("verified", verified);
        results.put("address_id", addressId.toString());
        if (!verified) results.put("error", "Address not found");
    }

    private void handleActivatePerson(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        if (personIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing person_id");
            return;
        }

        UUID personId = UUID.fromString(personIdStr);
        Optional<PersonDto> existingPerson = personService.getPersonById(personId);

        if (existingPerson.isEmpty()) {
            results.put("success", false);
            results.put("error", "Person not found");
            return;
        }

        PersonDto person = existingPerson.get();
        person.setActive(true);
        person.setVerified(true);

        Optional<PersonDto> updated = personService.updatePerson(personId, person);
        results.put("success", updated.isPresent());
        results.put("personId", personId.toString());
        if (updated.isPresent()) {
            results.put("message", "Person activated successfully");
        }
    }

    private void handleDeleteAllPersonData(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        if (personIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing person_id");
            return;
        }

        UUID personId = UUID.fromString(personIdStr);
        personService.deleteAllPersonData(personId);

        results.put("success", true);
        results.put("personId", personId.toString());
        results.put("message", "All person data deleted");
    }

    private void handleUpdatePersonReported(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        if (personIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing person_id");
            return;
        }

        UUID personId = UUID.fromString(personIdStr.trim());
        boolean updated = personService.updateReported(personId, true);

        results.put("success", updated);
        results.put("person_id", personId.toString());
        if (updated) {
            results.put("message", "Person reported status updated to true");
        } else {
            results.put("error", "Person not found");
        }
    }

    // ========== KYC Helper Methods ==========

    private void updateKycDataHelper(KycDto kycDto, String kycType, String documentId) {
        LocalDateTime now = LocalDateTime.now();

        switch (kycType != null ? kycType : "") {
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
            case "liveliness":
                kycDto.setLivelinessVerified(true);
                kycDto.setLivelinessVerificationDate(now);
                break;
            default:
                kycDto.setIdVerified(true);
                kycDto.setIdVerificationDate(now);
        }
    }

    private void deleteKycDataHelper(KycDto kycDto, String kycType, String documentId) {
        switch (kycType != null ? kycType : "") {
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
            case "liveliness":
                kycDto.setLivelinessVerified(false);
                kycDto.setLivelinessVerificationDate(null);
                break;
            default:
                kycDto.setIdVerified(false);
                kycDto.setIdVerificationDate(null);
        }
    }
}

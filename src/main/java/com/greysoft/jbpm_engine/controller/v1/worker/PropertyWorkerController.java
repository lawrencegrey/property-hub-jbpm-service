package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.property.*;
import com.greysoft.jbpm_engine.service.NotificationService;
import com.greysoft.jbpm_engine.service.PersonService;
import com.greysoft.jbpm_engine.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * REST endpoints for property worker operations.
 * Called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker methods:
 *  - update-property-data, delete-property, activate-property, offer-property
 *  - add-offer, update-offer, update-offer-with-process-instance, delete-offer
 *  - create-engagement, add-engagement
 *  - create-engagement-party, add-engagement-party, delete-engagement-party, update-engagement-party
 *  - get-offer-participants, get-request-participants
 *  - notify-participants
 */
@RestController
@RequestMapping("/api/v1/workers/property")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Worker", description = "REST endpoints for property workflow operations (called by jBPM)")
public class PropertyWorkerController {

    private final PropertyService propertyService;
    private final PersonService personService;
    private final NotificationService notificationService;

    // ==================== Property CRUD ====================

    @PostMapping("/update-property-data")
    @Operation(summary = "Update property data based on update_type")
    public ResponseEntity<Map<String, Object>> updatePropertyData(@RequestBody Map<String, Object> variables) {
        log.info("[update-property-data] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID propertyId = UUID.fromString(propertyIdObj.toString());
            Optional<PropertyDto> currentProperty = propertyService.getPropertyById(propertyId);
            if (currentProperty.isEmpty()) {
                result.put("success", false);
                result.put("error", "Property not found");
                return ResponseEntity.ok(result);
            }

            PropertyDto property = currentProperty.get();
            propertyService.updateProperty(propertyId, property);

            result.put("success", true);
            result.put("property_id", propertyId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-property-data] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/delete-property")
    @Operation(summary = "Delete a property and all related data")
    public ResponseEntity<Map<String, Object>> deleteProperty(@RequestBody Map<String, Object> variables) {
        log.info("[delete-property] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID propertyId = UUID.fromString(propertyIdObj.toString());
            boolean deleted = propertyService.deletePropertyAndAllRelatedData(propertyId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("property_id", propertyId.toString());
            if (!deleted) result.put("error", "Failed to delete property");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-property] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/activate-property")
    @Operation(summary = "Activate a property (set verified, active, listing status)")
    public ResponseEntity<Map<String, Object>> activateProperty(@RequestBody Map<String, Object> variables) {
        log.info("[activate-property] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID propertyId = UUID.fromString(propertyIdObj.toString());
            Optional<PropertyDto> currentProperty = propertyService.getPropertyById(propertyId);
            if (currentProperty.isEmpty()) {
                result.put("success", false);
                result.put("error", "Property not found");
                return ResponseEntity.ok(result);
            }

            PropertyDto property = currentProperty.get();
            property.setVerified(true);
            property.setActive(true);
            property.setListingStatus("ACTIVE");

            propertyService.updateProperty(propertyId, property);

            result.put("success", true);
            result.put("activated", true);
            result.put("property_id", propertyId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[activate-property] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Offer Operations ====================

    @PostMapping("/offer-property")
    @Operation(summary = "Update property offer status (under offer / accepting offers)")
    public ResponseEntity<Map<String, Object>> offerProperty(@RequestBody Map<String, Object> variables) {
        log.info("[offer-property] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID propertyId = UUID.fromString(propertyIdObj.toString());
            Object offerStatusObj = variables.get("offer_status");
            boolean offerStatus = offerStatusObj != null ? Boolean.parseBoolean(offerStatusObj.toString()) : true;

            PropertyDto updated = propertyService.updateOfferStatus(propertyId, offerStatus, !offerStatus);

            result.put("success", true);
            result.put("under_offer", offerStatus);
            result.put("accepting_offers", !offerStatus);
            result.put("property_id", propertyId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[offer-property] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/add-offer")
    @Operation(summary = "Create a new offer on a property")
    public ResponseEntity<Map<String, Object>> addOffer(@RequestBody Map<String, Object> variables) {
        log.info("[add-offer] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            Object personIdObj = variables.get("person_id");
            Object offerAmountObj = variables.get("offer_amount");
            Object currencyObj = variables.get("currency");
            Object loanFinancedObj = variables.get("loan_financed");
            Object usePlatformObj = variables.get("use_platform");
            Object processInstanceIdObj = variables.get("process_instance_id");

            if (propertyIdObj == null || personIdObj == null || offerAmountObj == null || currencyObj == null) {
                result.put("success", false);
                result.put("error", "Missing required fields: property_id, person_id, offer_amount, currency");
                return ResponseEntity.badRequest().body(result);
            }

            OfferDto offerDto = OfferDto.builder()
                    .propertyId(UUID.fromString(propertyIdObj.toString()))
                    .personId(UUID.fromString(personIdObj.toString()))
                    .offerAmount(new BigDecimal(offerAmountObj.toString()))
                    .currency(currencyObj.toString())
                    .loanFinanced(loanFinancedObj != null && Boolean.parseBoolean(loanFinancedObj.toString()))
                    .usePlatform(usePlatformObj != null && Boolean.parseBoolean(usePlatformObj.toString()))
                    .processInstanceId(processInstanceIdObj != null ? processInstanceIdObj.toString() : null)
                    .accepted(true)
                    .build();

            Optional<OfferDto> created = propertyService.addOffer(offerDto);
            if (created.isPresent()) {
                result.put("success", true);
                result.put("offer_id", created.get().getId() != null ? created.get().getId().toString() : null);
                result.put("property_id", propertyIdObj.toString());
            } else {
                result.put("success", false);
                result.put("error", "Failed to create offer");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[add-offer] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/update-offer")
    @Operation(summary = "Update an offer (set accepted=true)")
    public ResponseEntity<Map<String, Object>> updateOffer(@RequestBody Map<String, Object> variables) {
        log.info("[update-offer] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object offerIdObj = variables.get("offer_id");
            if (offerIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing offer_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID offerId = UUID.fromString(offerIdObj.toString());
            Optional<OfferDto> currentOffer = propertyService.getOfferById(offerId);
            if (currentOffer.isEmpty()) {
                result.put("success", false);
                result.put("error", "Offer not found");
                return ResponseEntity.ok(result);
            }

            OfferDto offer = currentOffer.get();
            offer.setAccepted(true);

            // Optionally update process instance ID
            Object processInstanceIdObj = variables.get("process_instance_id");
            if (processInstanceIdObj != null) {
                offer.setProcessInstanceId(processInstanceIdObj.toString());
            }

            Optional<OfferDto> updated = propertyService.updateOffer(offerId, offer);
            result.put("success", updated.isPresent());
            result.put("updated", updated.isPresent());
            result.put("offer_id", offerId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-offer] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/update-offer-with-process-instance")
    @Operation(summary = "Update offer with process instance ID only")
    public ResponseEntity<Map<String, Object>> updateOfferWithProcessInstance(@RequestBody Map<String, Object> variables) {
        log.info("[update-offer-with-process-instance] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object offerIdObj = variables.get("offer_id");
            Object processInstanceIdObj = variables.get("process_instance_id");

            if (offerIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing offer_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID offerId = UUID.fromString(offerIdObj.toString());
            Optional<OfferDto> currentOffer = propertyService.getOfferById(offerId);
            if (currentOffer.isEmpty()) {
                result.put("success", false);
                result.put("error", "Offer not found");
                return ResponseEntity.ok(result);
            }

            OfferDto offer = currentOffer.get();
            if (processInstanceIdObj != null) {
                offer.setProcessInstanceId(processInstanceIdObj.toString());
            }

            Optional<OfferDto> updated = propertyService.updateOffer(offerId, offer);
            result.put("success", updated.isPresent());
            result.put("updated", updated.isPresent());
            result.put("offer_id", offerId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-offer-with-process-instance] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/delete-offer")
    @Operation(summary = "Delete an offer")
    public ResponseEntity<Map<String, Object>> deleteOffer(@RequestBody Map<String, Object> variables) {
        log.info("[delete-offer] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object offerIdObj = variables.get("offer_id");
            if (offerIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing offer_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID offerId = UUID.fromString(offerIdObj.toString());
            boolean deleted = propertyService.deleteOffer(offerId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("offer_id", offerId.toString());
            if (!deleted) result.put("error", "Failed to delete offer");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-offer] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Engagement Operations ====================

    @PostMapping("/create-engagement")
    @Operation(summary = "Create an engagement for a property transaction")
    public ResponseEntity<Map<String, Object>> createEngagement(@RequestBody Map<String, Object> variables) {
        log.info("[create-engagement] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object propertyIdObj = variables.get("property_id");
            Object transactionTypeObj = variables.get("transaction_type");
            Object propertyTypeObj = variables.get("property_type");
            Object offerIdObj = variables.get("offer_id");

            if (propertyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing property_id");
                return ResponseEntity.badRequest().body(result);
            }

            EngagementDto engagement = new EngagementDto();
            engagement.setPropertyId(UUID.fromString(propertyIdObj.toString()));
            if (transactionTypeObj != null) engagement.setTransactionType(transactionTypeObj.toString());
            if (propertyTypeObj != null) engagement.setPropertyType(propertyTypeObj.toString());
            if (offerIdObj != null) engagement.setOfferId(UUID.fromString(offerIdObj.toString()));
            engagement.setActive(true);

            EngagementDto created = propertyService.createEngagement(engagement);

            result.put("success", true);
            result.put("created", true);
            result.put("engagement_id", created.getId() != null ? created.getId().toString() : null);
            result.put("property_id", propertyIdObj.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[create-engagement] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/add-engagement")
    @Operation(summary = "Add engagement (alias for create-engagement with defaults)")
    public ResponseEntity<Map<String, Object>> addEngagement(@RequestBody Map<String, Object> variables) {
        // Same logic as create-engagement, just an alias
        return createEngagement(variables);
    }

    // ==================== Engagement Party Operations ====================

    @PostMapping("/create-engagement-party")
    @Operation(summary = "Create an engagement party")
    public ResponseEntity<Map<String, Object>> createEngagementParty(@RequestBody Map<String, Object> variables) {
        log.info("[create-engagement-party] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object engagementIdObj = variables.get("engagement_id");
            Object propertyIdObj = variables.get("property_id");
            Object personIdObj = variables.get("person_id");
            Object personTypeObj = variables.get("person_type");

            if (engagementIdObj == null || propertyIdObj == null || personIdObj == null || personTypeObj == null) {
                result.put("success", false);
                result.put("error", "Missing required fields: engagement_id, property_id, person_id, person_type");
                return ResponseEntity.badRequest().body(result);
            }

            EngagementPartyDto party = new EngagementPartyDto();
            party.setEngagementId(UUID.fromString(engagementIdObj.toString()));
            party.setPropertyId(UUID.fromString(propertyIdObj.toString()));
            party.setPersonId(UUID.fromString(personIdObj.toString()));
            party.setPersonType(personTypeObj.toString());
            party.setActive(true);
            party.setAccepted(true);
            party.setCreatedBy(UUID.fromString(personIdObj.toString()));

            Object acceptedObj = variables.get("accepted");
            if (acceptedObj != null) party.setAccepted(Boolean.parseBoolean(acceptedObj.toString()));

            Object representsObj = variables.get("represents");
            if (representsObj != null) party.setRepresents(representsObj.toString());

            EngagementPartyDto created = propertyService.createEngagementParty(party);

            result.put("success", true);
            result.put("created", true);
            result.put("engagement_party_id", created.getId() != null ? created.getId().toString() : null);
            result.put("engagement_id", engagementIdObj.toString());
            result.put("property_id", propertyIdObj.toString());
            result.put("person_id", personIdObj.toString());
            result.put("person_type", personTypeObj.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[create-engagement-party] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/add-engagement-party")
    @Operation(summary = "Add engagement party (alias)")
    public ResponseEntity<Map<String, Object>> addEngagementParty(@RequestBody Map<String, Object> variables) {
        return createEngagementParty(variables);
    }

    @PostMapping("/delete-engagement-party")
    @Operation(summary = "Delete an engagement party")
    public ResponseEntity<Map<String, Object>> deleteEngagementParty(@RequestBody Map<String, Object> variables) {
        log.info("[delete-engagement-party] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object engagementPartyIdObj = variables.get("engagement_party_id");
            if (engagementPartyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing engagement_party_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID engagementPartyId = UUID.fromString(engagementPartyIdObj.toString());
            boolean deleted = propertyService.deleteEngagementParty(engagementPartyId);

            result.put("success", deleted);
            result.put("deleted", deleted);
            result.put("engagement_party_id", engagementPartyId.toString());
            if (!deleted) result.put("error", "Engagement party not found");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[delete-engagement-party] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/update-engagement-party")
    @Operation(summary = "Update engagement party (set accepted=true, active=true)")
    public ResponseEntity<Map<String, Object>> updateEngagementParty(@RequestBody Map<String, Object> variables) {
        log.info("[update-engagement-party] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object engagementPartyIdObj = variables.get("engagement_party_id");
            if (engagementPartyIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing engagement_party_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID engagementPartyId = UUID.fromString(engagementPartyIdObj.toString());
            Optional<EngagementPartyDto> current = propertyService.getEngagementPartyById(engagementPartyId);
            if (current.isEmpty()) {
                result.put("success", false);
                result.put("error", "Engagement party not found");
                return ResponseEntity.ok(result);
            }

            EngagementPartyDto party = current.get();
            party.setAccepted(true);
            party.setActive(true);

            Optional<EngagementPartyDto> updated = propertyService.updateEngagementParty(engagementPartyId, party);
            result.put("success", updated.isPresent());
            result.put("updated", updated.isPresent());
            result.put("engagement_party_id", engagementPartyId.toString());
            result.put("accepted", true);
            result.put("active", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[update-engagement-party] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Participant Lookups ====================

    @PostMapping("/get-offer-participants")
    @Operation(summary = "Get buyer and seller details for an offer")
    public ResponseEntity<Map<String, Object>> getOfferParticipants(@RequestBody Map<String, Object> variables) {
        log.info("[get-offer-participants] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String buyerIdStr = (String) variables.get("buyer_id");
            String sellerIdStr = (String) variables.get("seller_id");

            if (buyerIdStr == null || buyerIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Buyer ID is required");
                return ResponseEntity.badRequest().body(result);
            }
            if (sellerIdStr == null || sellerIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Seller ID is required");
                return ResponseEntity.badRequest().body(result);
            }

            Optional<PersonDto> buyerOpt = personService.getPersonById(UUID.fromString(buyerIdStr));
            Optional<PersonDto> sellerOpt = personService.getPersonById(UUID.fromString(sellerIdStr));

            if (buyerOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Buyer not found");
                return ResponseEntity.ok(result);
            }
            if (sellerOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Seller not found");
                return ResponseEntity.ok(result);
            }

            PersonDto buyer = buyerOpt.get();
            PersonDto seller = sellerOpt.get();

            result.put("buyer_first_name", buyer.getFirstName());
            result.put("buyer_last_name", buyer.getLastName());
            result.put("buyer_email", buyer.getEmail());
            result.put("buyer_mobile", buyer.getMobileNumber());
            result.put("buyer_token", buyer.getDeviceToken());

            result.put("seller_first_name", seller.getFirstName());
            result.put("seller_last_name", seller.getLastName());
            result.put("seller_email", seller.getEmail());
            result.put("seller_mobile", seller.getMobileNumber());
            result.put("seller_token", seller.getDeviceToken());

            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[get-offer-participants] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/get-request-participants")
    @Operation(summary = "Get requesting person and professional details")
    public ResponseEntity<Map<String, Object>> getRequestParticipants(@RequestBody Map<String, Object> variables) {
        log.info("[get-request-participants] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            String personIdStr = (String) variables.get("person_id");
            String partyIdStr = (String) variables.get("party_id");

            if (personIdStr == null || personIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Requesting person ID is required");
                return ResponseEntity.badRequest().body(result);
            }
            if (partyIdStr == null || partyIdStr.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Professional ID is required");
                return ResponseEntity.badRequest().body(result);
            }

            Optional<PersonDto> personOpt = personService.getPersonById(UUID.fromString(personIdStr));
            Optional<PersonDto> professionalOpt = personService.getPersonById(UUID.fromString(partyIdStr));

            if (personOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Requesting person not found");
                return ResponseEntity.ok(result);
            }
            if (professionalOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Professional not found");
                return ResponseEntity.ok(result);
            }

            PersonDto person = personOpt.get();
            PersonDto professional = professionalOpt.get();

            result.put("first_name", person.getFirstName());
            result.put("last_name", person.getLastName());
            result.put("email", person.getEmail());
            result.put("mobile", person.getMobileNumber());
            result.put("token", person.getDeviceToken());

            result.put("party_first_name", professional.getFirstName());
            result.put("party_last_name", professional.getLastName());
            result.put("party_email", professional.getEmail());
            result.put("party_mobile", professional.getMobileNumber());
            result.put("party_token", professional.getDeviceToken());

            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[get-request-participants] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== Notifications ====================

    @PostMapping("/notify-participants")
    @Operation(summary = "Notify all participants in an engagement")
    public ResponseEntity<Map<String, Object>> notifyParticipants(@RequestBody Map<String, Object> variables) {
        log.info("[notify-participants] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object engagementIdObj = variables.get("engagement_id");
            Object subjectObj = variables.get("subject");
            Object bodyObj = variables.get("body");

            if (engagementIdObj == null || subjectObj == null || bodyObj == null) {
                result.put("success", false);
                result.put("error", "Missing required fields: engagement_id, subject, body");
                return ResponseEntity.badRequest().body(result);
            }

            UUID engagementId = UUID.fromString(engagementIdObj.toString());
            String subject = subjectObj.toString();
            String body = bodyObj.toString();

            List<EngagementPartyDto> parties = propertyService.getEngagementPartiesByEngagementId(engagementId);

            int notificationsSent = 0;
            List<String> notifiedPersonIds = new ArrayList<>();

            for (EngagementPartyDto party : parties) {
                try {
                    UUID personId = party.getPersonId();
                    Optional<PersonDto> personOpt = personService.getPersonById(personId);
                    if (personOpt.isEmpty()) continue;

                    PersonDto person = personOpt.get();

                    // Send email
                    if (person.getEmail() != null) {
                        notificationService.sendEmail(person.getEmail(), subject, body);
                    }
                    // Send push notification
                    if (person.getDeviceToken() != null && !person.getDeviceToken().trim().isEmpty()) {
                        notificationService.sendPushNotification(person.getDeviceToken(), subject, body);
                    }
                    // Send SMS
                    if (person.getMobileNumber() != null) {
                        notificationService.sendSms(person.getMobileNumber(), subject);
                    }

                    notificationsSent++;
                    notifiedPersonIds.add(personId.toString());

                } catch (Exception e) {
                    log.error("[notify-participants] Error notifying personId={}: {}", party.getPersonId(), e.getMessage());
                }
            }

            result.put("success", true);
            result.put("notifications_sent", notificationsSent);
            result.put("notified_person_ids", notifiedPersonIds);
            result.put("engagement_id", engagementId.toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[notify-participants] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

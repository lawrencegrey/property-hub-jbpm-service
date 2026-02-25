package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.property.*;
import com.greysoft.jbpm_engine.service.NotificationService;
import com.greysoft.jbpm_engine.service.PersonService;
import com.greysoft.jbpm_engine.service.PropertyService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * jBPM WorkItemHandler for property-related workflow tasks.
 * <p>
 * Handles task types:
 * update-property-data, delete-property, activate-property, offer-property,
 * add-offer, update-offer, update-offer-with-process-instance, delete-offer,
 * create-engagement, add-engagement, create-engagement-party, add-engagement-party,
 * delete-engagement-party, update-engagement-party, get-offer-participants,
 * get-request-participants, notify-participants
 */
public class PropertyWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(PropertyWorkItemHandler.class);

    private final PropertyService propertyService;
    private final PersonService personService;
    private final NotificationService notificationService;

    public PropertyWorkItemHandler(PropertyService propertyService,
                                   PersonService personService,
                                   NotificationService notificationService) {
        this.propertyService = propertyService;
        this.personService = personService;
        this.notificationService = notificationService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        if (taskType == null) taskType = workItem.getName();
        log.info("[PropertyWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            switch (taskType) {
                case "update-property-data":
                    handleUpdatePropertyData(workItem, results);
                    break;
                case "delete-property":
                    handleDeleteProperty(workItem, results);
                    break;
                case "activate-property":
                    handleActivateProperty(workItem, results);
                    break;
                case "offer-property":
                    handleOfferProperty(workItem, results);
                    break;
                case "add-offer":
                    handleAddOffer(workItem, results);
                    break;
                case "update-offer":
                    handleUpdateOffer(workItem, results);
                    break;
                case "update-offer-with-process-instance":
                    handleUpdateOfferWithProcessInstance(workItem, results);
                    break;
                case "delete-offer":
                    handleDeleteOffer(workItem, results);
                    break;
                case "create-engagement":
                case "add-engagement":
                    handleCreateEngagement(workItem, results);
                    break;
                case "create-engagement-party":
                case "add-engagement-party":
                    handleCreateEngagementParty(workItem, results);
                    break;
                case "delete-engagement-party":
                    handleDeleteEngagementParty(workItem, results);
                    break;
                case "update-engagement-party":
                    handleUpdateEngagementParty(workItem, results);
                    break;
                case "get-offer-participants":
                    handleGetOfferParticipants(workItem, results);
                    break;
                case "get-request-participants":
                    handleGetRequestParticipants(workItem, results);
                    break;
                case "notify-participants":
                    handleNotifyParticipants(workItem, results);
                    break;
                default:
                    log.warn("[PropertyWorkItemHandler] Unknown taskType: {}", taskType);
                    results.put("success", false);
                    results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[PropertyWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[PropertyWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementations ==========

    private void handleUpdatePropertyData(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        Optional<PropertyDto> currentProperty = propertyService.getPropertyById(propertyId);

        if (currentProperty.isEmpty()) {
            results.put("success", false);
            results.put("error", "Property not found");
            return;
        }

        PropertyDto property = currentProperty.get();
        String updateType = (String) workItem.getParameter("update_type");

        if ("listing_update".equals(updateType)) {
            Object listingStatus = workItem.getParameter("listing_status");
            if (listingStatus != null) property.setListingStatus(listingStatus.toString());
        } else if ("details_update".equals(updateType)) {
            Object title = workItem.getParameter("title");
            Object description = workItem.getParameter("description");
            if (title != null) property.setTitle(title.toString());
            if (description != null) property.setDescription(description.toString());
        }

        propertyService.updateProperty(propertyId, property)
                .ifPresent(updated -> log.info("[update-property-data] Updated propertyId={}", propertyId));

        results.put("success", true);
        results.put("property_id", propertyId.toString());
    }

    private void handleDeleteProperty(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        boolean deleted = propertyService.deletePropertyAndAllRelatedData(propertyId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("property_id", propertyId.toString());
        if (!deleted) results.put("error", "Failed to delete property");
    }

    private void handleActivateProperty(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        Optional<PropertyDto> currentProperty = propertyService.getPropertyById(propertyId);

        if (currentProperty.isEmpty()) {
            results.put("success", false);
            results.put("error", "Property not found");
            return;
        }

        PropertyDto property = currentProperty.get();
        property.setVerified(true);
        property.setActive(true);
        property.setListingStatus("ACTIVE");

        propertyService.updateProperty(propertyId, property)
                .ifPresent(updated -> log.info("[activate-property] Activated propertyId={}", propertyId));

        results.put("success", true);
        results.put("property_id", propertyId.toString());
    }

    private void handleOfferProperty(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        Object offerStatusObj = workItem.getParameter("offer_status");
        boolean offerStatus = offerStatusObj != null ? Boolean.parseBoolean(offerStatusObj.toString()) : true;

        PropertyDto updatedProperty = propertyService.updateOfferStatus(propertyId, offerStatus, !offerStatus);

        results.put("success", true);
        results.put("under_offer", offerStatus);
        results.put("accepting_offers", !offerStatus);
        results.put("property_id", propertyId.toString());
    }

    private void handleAddOffer(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        Object personIdObj = workItem.getParameter("person_id");
        Object offerAmountObj = workItem.getParameter("offer_amount");
        Object currencyObj = workItem.getParameter("currency");
        Object loanFinancedObj = workItem.getParameter("loan_financed");
        Object usePlatformObj = workItem.getParameter("use_platform");

        if (propertyIdObj == null || personIdObj == null || offerAmountObj == null) {
            results.put("success", false);
            results.put("error", "Missing required fields: property_id, person_id, offer_amount");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        UUID personId = UUID.fromString(personIdObj.toString());
        BigDecimal offerAmount = new BigDecimal(offerAmountObj.toString());

        String processInstanceId = (String) workItem.getParameter("process_instance_id");

        OfferDto offerDto = OfferDto.builder()
                .propertyId(propertyId)
                .personId(personId)
                .offerAmount(offerAmount)
                .currency(currencyObj != null ? currencyObj.toString() : "JMD")
                .loanFinanced(loanFinancedObj != null && Boolean.parseBoolean(loanFinancedObj.toString()))
                .usePlatform(usePlatformObj != null && Boolean.parseBoolean(usePlatformObj.toString()))
                .processInstanceId(processInstanceId)
                .accepted(true)
                .build();

        Optional<OfferDto> createdOffer = propertyService.addOffer(offerDto);

        if (createdOffer.isPresent()) {
            OfferDto offer = createdOffer.get();
            results.put("success", true);
            results.put("offer_id", offer.getId() != null ? offer.getId().toString() : "");
            results.put("property_id", propertyId.toString());
            results.put("person_id", personId.toString());
        } else {
            results.put("success", false);
            results.put("error", "Failed to create offer");
        }
    }

    private void handleUpdateOffer(WorkItem workItem, Map<String, Object> results) {
        Object offerIdObj = workItem.getParameter("offer_id");
        if (offerIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing offer_id");
            return;
        }

        UUID offerId = UUID.fromString(offerIdObj.toString());
        Optional<OfferDto> currentOffer = propertyService.getOfferById(offerId);

        if (currentOffer.isEmpty()) {
            results.put("success", false);
            results.put("error", "Offer not found");
            return;
        }

        OfferDto offer = currentOffer.get();
        offer.setAccepted(true);

        String processInstanceId = (String) workItem.getParameter("process_instance_id");
        if (processInstanceId != null) {
            offer.setProcessInstanceId(processInstanceId);
        }

        Optional<OfferDto> updatedOffer = propertyService.updateOffer(offerId, offer);
        results.put("success", updatedOffer.isPresent());
        results.put("updated", updatedOffer.isPresent());
        results.put("offer_id", offerId.toString());
    }

    private void handleUpdateOfferWithProcessInstance(WorkItem workItem, Map<String, Object> results) {
        Object offerIdObj = workItem.getParameter("offer_id");
        if (offerIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing offer_id");
            return;
        }

        UUID offerId = UUID.fromString(offerIdObj.toString());
        Optional<OfferDto> currentOffer = propertyService.getOfferById(offerId);

        if (currentOffer.isEmpty()) {
            results.put("success", false);
            results.put("error", "Offer not found");
            return;
        }

        OfferDto offer = currentOffer.get();
        String processInstanceId = (String) workItem.getParameter("process_instance_id");
        if (processInstanceId != null) {
            offer.setProcessInstanceId(processInstanceId);
        }

        Optional<OfferDto> updatedOffer = propertyService.updateOffer(offerId, offer);
        results.put("success", updatedOffer.isPresent());
        results.put("updated", updatedOffer.isPresent());
        results.put("offer_id", offerId.toString());
    }

    private void handleDeleteOffer(WorkItem workItem, Map<String, Object> results) {
        Object offerIdObj = workItem.getParameter("offer_id");
        if (offerIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing offer_id");
            return;
        }

        UUID offerId = UUID.fromString(offerIdObj.toString());
        boolean deleted = propertyService.deleteOffer(offerId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("offer_id", offerId.toString());
        if (!deleted) results.put("error", "Offer not found or failed to delete");
    }

    private void handleCreateEngagement(WorkItem workItem, Map<String, Object> results) {
        Object propertyIdObj = workItem.getParameter("property_id");
        Object transactionTypeObj = workItem.getParameter("transaction_type");
        Object propertyTypeObj = workItem.getParameter("property_type");
        Object offerIdObj = workItem.getParameter("offer_id");

        if (propertyIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing property_id");
            return;
        }

        UUID propertyId = UUID.fromString(propertyIdObj.toString());

        EngagementDto engagement = new EngagementDto();
        engagement.setPropertyId(propertyId);
        engagement.setTransactionType(transactionTypeObj != null ? transactionTypeObj.toString() : "SALE");
        engagement.setPropertyType(propertyTypeObj != null ? propertyTypeObj.toString() : "REAL_ESTATE");
        engagement.setActive(true);

        if (offerIdObj != null && !offerIdObj.toString().trim().isEmpty()) {
            engagement.setOfferId(UUID.fromString(offerIdObj.toString()));
        }

        EngagementDto created = propertyService.createEngagement(engagement);

        if (created != null) {
            results.put("success", true);
            results.put("created", true);
            results.put("engagement_id", created.getId() != null ? created.getId().toString() : "");
            results.put("property_id", propertyId.toString());
        } else {
            results.put("success", false);
            results.put("error", "Failed to create engagement");
        }
    }

    private void handleCreateEngagementParty(WorkItem workItem, Map<String, Object> results) {
        Object engagementIdObj = workItem.getParameter("engagement_id");
        Object propertyIdObj = workItem.getParameter("property_id");
        Object personIdObj = workItem.getParameter("person_id");
        Object personTypeObj = workItem.getParameter("person_type");

        if (engagementIdObj == null || propertyIdObj == null || personIdObj == null || personTypeObj == null) {
            results.put("success", false);
            results.put("error", "Missing required fields: engagement_id, property_id, person_id, person_type");
            return;
        }

        UUID engagementId = UUID.fromString(engagementIdObj.toString());
        UUID propertyId = UUID.fromString(propertyIdObj.toString());
        UUID personId = UUID.fromString(personIdObj.toString());

        EngagementPartyDto party = new EngagementPartyDto();
        party.setEngagementId(engagementId);
        party.setPropertyId(propertyId);
        party.setPersonId(personId);
        party.setPersonType(personTypeObj.toString());
        party.setRepresents(personTypeObj.toString());
        party.setActive(true);
        party.setAccepted(true);
        party.setCreatedBy(personId);

        Object acceptedObj = workItem.getParameter("accepted");
        if (acceptedObj != null) {
            party.setAccepted(Boolean.parseBoolean(acceptedObj.toString()));
        }

        Object representsObj = workItem.getParameter("represents");
        if (representsObj != null) {
            party.setRepresents(representsObj.toString());
        }

        EngagementPartyDto created = propertyService.createEngagementParty(party);

        if (created != null) {
            results.put("success", true);
            results.put("created", true);
            results.put("engagement_party_id", created.getId() != null ? created.getId().toString() : "");
            results.put("engagement_id", engagementId.toString());
            results.put("property_id", propertyId.toString());
            results.put("person_id", personId.toString());
            results.put("person_type", personTypeObj.toString());
        } else {
            results.put("success", false);
            results.put("error", "Failed to create engagement party");
        }
    }

    private void handleDeleteEngagementParty(WorkItem workItem, Map<String, Object> results) {
        Object epIdObj = workItem.getParameter("engagement_party_id");
        if (epIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing engagement_party_id");
            return;
        }

        UUID engagementPartyId = UUID.fromString(epIdObj.toString());
        boolean deleted = propertyService.deleteEngagementParty(engagementPartyId);

        results.put("success", deleted);
        results.put("deleted", deleted);
        results.put("engagement_party_id", engagementPartyId.toString());
        if (!deleted) results.put("error", "EngagementParty not found");
    }

    private void handleUpdateEngagementParty(WorkItem workItem, Map<String, Object> results) {
        Object epIdObj = workItem.getParameter("engagement_party_id");
        if (epIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing engagement_party_id");
            return;
        }

        UUID engagementPartyId = UUID.fromString(epIdObj.toString());
        Optional<EngagementPartyDto> current = propertyService.getEngagementPartyById(engagementPartyId);

        if (current.isEmpty()) {
            results.put("success", false);
            results.put("error", "EngagementParty not found");
            return;
        }

        EngagementPartyDto party = current.get();
        party.setAccepted(true);
        party.setActive(true);

        Optional<EngagementPartyDto> updated = propertyService.updateEngagementParty(engagementPartyId, party);
        results.put("success", updated.isPresent());
        results.put("updated", updated.isPresent());
        results.put("engagement_party_id", engagementPartyId.toString());
    }

    private void handleGetOfferParticipants(WorkItem workItem, Map<String, Object> results) {
        String buyerIdStr = (String) workItem.getParameter("buyer_id");
        String sellerIdStr = (String) workItem.getParameter("seller_id");

        if (buyerIdStr == null || sellerIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing buyer_id or seller_id");
            return;
        }

        UUID buyerId = UUID.fromString(buyerIdStr);
        UUID sellerId = UUID.fromString(sellerIdStr);

        Optional<PersonDto> buyerOpt = personService.getPersonById(buyerId);
        Optional<PersonDto> sellerOpt = personService.getPersonById(sellerId);

        if (buyerOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Buyer not found");
            return;
        }
        if (sellerOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Seller not found");
            return;
        }

        PersonDto buyer = buyerOpt.get();
        PersonDto seller = sellerOpt.get();

        results.put("buyer_first_name", buyer.getFirstName());
        results.put("buyer_last_name", buyer.getLastName());
        results.put("buyer_email", buyer.getEmail());
        results.put("buyer_mobile", buyer.getMobileNumber());
        results.put("buyer_token", buyer.getDeviceToken());

        results.put("seller_first_name", seller.getFirstName());
        results.put("seller_last_name", seller.getLastName());
        results.put("seller_email", seller.getEmail());
        results.put("seller_mobile", seller.getMobileNumber());
        results.put("seller_token", seller.getDeviceToken());

        results.put("success", true);
    }

    private void handleGetRequestParticipants(WorkItem workItem, Map<String, Object> results) {
        String personIdStr = (String) workItem.getParameter("person_id");
        String partyIdStr = (String) workItem.getParameter("party_id");

        if (personIdStr == null || partyIdStr == null) {
            results.put("success", false);
            results.put("error", "Missing person_id or party_id");
            return;
        }

        UUID personId = UUID.fromString(personIdStr);
        UUID partyId = UUID.fromString(partyIdStr);

        Optional<PersonDto> personOpt = personService.getPersonById(personId);
        Optional<PersonDto> partyOpt = personService.getPersonById(partyId);

        if (personOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Requesting person not found");
            return;
        }
        if (partyOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Professional not found");
            return;
        }

        PersonDto person = personOpt.get();
        PersonDto party = partyOpt.get();

        results.put("first_name", person.getFirstName());
        results.put("last_name", person.getLastName());
        results.put("email", person.getEmail());
        results.put("mobile", person.getMobileNumber());
        results.put("token", person.getDeviceToken());

        results.put("party_first_name", party.getFirstName());
        results.put("party_last_name", party.getLastName());
        results.put("party_email", party.getEmail());
        results.put("party_mobile", party.getMobileNumber());
        results.put("party_token", party.getDeviceToken());

        results.put("success", true);
    }

    private void handleNotifyParticipants(WorkItem workItem, Map<String, Object> results) {
        Object engagementIdObj = workItem.getParameter("engagement_id");
        Object subjectObj = workItem.getParameter("subject");
        Object bodyObj = workItem.getParameter("body");

        if (engagementIdObj == null || subjectObj == null || bodyObj == null) {
            results.put("success", false);
            results.put("error", "Missing engagement_id, subject, or body");
            return;
        }

        UUID engagementId = UUID.fromString(engagementIdObj.toString());
        String subject = subjectObj.toString();
        String body = bodyObj.toString();

        List<EngagementPartyDto> parties = propertyService.getEngagementPartiesByEngagementId(engagementId);

        if (parties.isEmpty()) {
            results.put("success", true);
            results.put("notifications_sent", 0);
            results.put("message", "No participants found to notify");
            return;
        }

        int notificationsSent = 0;
        List<String> notifiedPersonIds = new ArrayList<>();

        for (EngagementPartyDto party : parties) {
            try {
                UUID personId = party.getPersonId();
                Optional<PersonDto> personOpt = personService.getPersonById(personId);

                if (personOpt.isEmpty()) {
                    log.warn("[notify-participants] Person not found for personId={}", personId);
                    continue;
                }

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

        results.put("success", true);
        results.put("notifications_sent", notificationsSent);
        results.put("notified_person_ids", notifiedPersonIds);
        results.put("engagement_id", engagementId.toString());
    }
}

package com.greysoft.jbpm_engine.workitemhandler;

import com.greysoft.jbpm_engine.dto.property.PropertyLeaseDto;
import com.greysoft.jbpm_engine.dto.property.PropertyRentalDto;
import com.greysoft.jbpm_engine.dto.property.PropertySalesDto;
import com.greysoft.jbpm_engine.service.PropertyService;
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
 * jBPM WorkItemHandler for agreement-related workflow tasks.
 * <p>
 * Handles task types: activate-rental, activate-sale, activate-lease
 */
public class AgreementWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(AgreementWorkItemHandler.class);

    private final PropertyService propertyService;

    public AgreementWorkItemHandler(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskType = (String) workItem.getParameter("TaskType");
        if (taskType == null) taskType = workItem.getName();
        log.info("[AgreementWorkItemHandler] Executing taskType={}, workItemId={}", taskType, workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            switch (taskType) {
                case "activate-rental":
                    handleActivateRental(workItem, results);
                    break;
                case "activate-sale":
                    handleActivateSale(workItem, results);
                    break;
                case "activate-lease":
                    handleActivateLease(workItem, results);
                    break;
                default:
                    log.warn("[AgreementWorkItemHandler] Unknown taskType: {}", taskType);
                    results.put("success", false);
                    results.put("error", "Unknown task type: " + taskType);
            }
        } catch (Exception e) {
            log.error("[AgreementWorkItemHandler] Error executing taskType={}: {}", taskType, e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("[AgreementWorkItemHandler] Aborting workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ========== Task Implementations ==========

    private void handleActivateRental(WorkItem workItem, Map<String, Object> results) {
        Object rentalIdObj = workItem.getParameter("rental_id");
        if (rentalIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing rental_id");
            return;
        }

        UUID rentalId = UUID.fromString(rentalIdObj.toString());
        log.info("[activate-rental] Processing rental activation for rental_id={}", rentalId);

        Optional<PropertyRentalDto> rentalOpt = propertyService.getPropertyRentalById(rentalId);
        if (rentalOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Rental not found for id: " + rentalId);
            return;
        }

        PropertyRentalDto rental = rentalOpt.get();
        rental.setRentalStatus("AVAILABLE");
        rental.setActive(true);

        Optional<PropertyRentalDto> updated = propertyService.updatePropertyRental(rentalId, rental);
        if (updated.isPresent()) {
            log.info("[activate-rental] Successfully activated rental for id={}", rentalId);
            results.put("success", true);
            results.put("rental_id", rentalId.toString());
            results.put("rental_status", "AVAILABLE");
        } else {
            results.put("success", false);
            results.put("error", "Failed to activate rental for id: " + rentalId);
        }
    }

    private void handleActivateSale(WorkItem workItem, Map<String, Object> results) {
        Object saleIdObj = workItem.getParameter("sale_id");
        if (saleIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing sale_id");
            return;
        }

        UUID saleId = UUID.fromString(saleIdObj.toString());
        log.info("[activate-sale] Processing sale activation for sale_id={}", saleId);

        Optional<PropertySalesDto> saleOpt = propertyService.getPropertySaleById(saleId);
        if (saleOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Sale not found for id: " + saleId);
            return;
        }

        PropertySalesDto sale = saleOpt.get();
        sale.setSaleStatus("LISTED");
        sale.setListingDate(LocalDateTime.now());
        sale.setActive(true);

        Optional<PropertySalesDto> updated = propertyService.updatePropertySale(saleId, sale);
        if (updated.isPresent()) {
            log.info("[activate-sale] Successfully activated sale for id={}", saleId);
            results.put("success", true);
            results.put("sale_id", saleId.toString());
            results.put("sale_status", "LISTED");
        } else {
            results.put("success", false);
            results.put("error", "Failed to activate sale for id: " + saleId);
        }
    }

    private void handleActivateLease(WorkItem workItem, Map<String, Object> results) {
        Object leaseIdObj = workItem.getParameter("lease_id");
        if (leaseIdObj == null) {
            results.put("success", false);
            results.put("error", "Missing lease_id");
            return;
        }

        UUID leaseId = UUID.fromString(leaseIdObj.toString());
        log.info("[activate-lease] Processing lease activation for lease_id={}", leaseId);

        Optional<PropertyLeaseDto> leaseOpt = propertyService.getPropertyLeaseById(leaseId);
        if (leaseOpt.isEmpty()) {
            results.put("success", false);
            results.put("error", "Lease not found for id: " + leaseId);
            return;
        }

        PropertyLeaseDto lease = leaseOpt.get();
        lease.setLeaseStatus("ACTIVE");
        lease.setActive(true);

        Optional<PropertyLeaseDto> updated = propertyService.updatePropertyLease(leaseId, lease);
        if (updated.isPresent()) {
            log.info("[activate-lease] Successfully activated lease for id={}", leaseId);
            results.put("success", true);
            results.put("lease_id", leaseId.toString());
            results.put("lease_status", "ACTIVE");
        } else {
            results.put("success", false);
            results.put("error", "Failed to activate lease for id: " + leaseId);
        }
    }
}

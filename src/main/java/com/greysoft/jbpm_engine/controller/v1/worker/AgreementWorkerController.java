package com.greysoft.jbpm_engine.controller.v1.worker;

import com.greysoft.jbpm_engine.dto.property.PropertyLeaseDto;
import com.greysoft.jbpm_engine.dto.property.PropertyRentalDto;
import com.greysoft.jbpm_engine.dto.property.PropertySalesDto;
import com.greysoft.jbpm_engine.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST endpoints for agreement worker operations.
 * Called by jBPM BPMN processes via the REST Work Item Handler.
 *
 * Replaces Camunda @JobWorker methods:
 *  - activate-rental, activate-sale, activate-lease
 */
@RestController
@RequestMapping("/api/v1/workers/agreement")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Agreement Worker", description = "REST endpoints for agreement workflow operations (called by jBPM)")
public class AgreementWorkerController {

    private final PropertyService propertyService;

    @PostMapping("/activate-rental")
    @Operation(summary = "Activate a rental agreement (set status=AVAILABLE, active=true)")
    public ResponseEntity<Map<String, Object>> activateRental(@RequestBody Map<String, Object> variables) {
        log.info("[activate-rental] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object rentalIdObj = variables.get("rental_id");
            if (rentalIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing rental_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID rentalId = UUID.fromString(rentalIdObj.toString());
            Optional<PropertyRentalDto> rentalOpt = propertyService.getPropertyRentalById(rentalId);
            if (rentalOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Rental not found: " + rentalId);
                return ResponseEntity.ok(result);
            }

            PropertyRentalDto rental = rentalOpt.get();
            rental.setRentalStatus("AVAILABLE");
            rental.setActive(true);

            Optional<PropertyRentalDto> updated = propertyService.updatePropertyRental(rentalId, rental);
            result.put("success", updated.isPresent());
            result.put("activated", updated.isPresent());
            result.put("rental_id", rentalId.toString());
            if (updated.isEmpty()) result.put("error", "Failed to activate rental");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[activate-rental] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/activate-sale")
    @Operation(summary = "Activate a sale listing (set status=LISTED, active=true, listingDate=now)")
    public ResponseEntity<Map<String, Object>> activateSale(@RequestBody Map<String, Object> variables) {
        log.info("[activate-sale] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object saleIdObj = variables.get("sale_id");
            if (saleIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing sale_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID saleId = UUID.fromString(saleIdObj.toString());
            Optional<PropertySalesDto> saleOpt = propertyService.getPropertySaleById(saleId);
            if (saleOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Sale not found: " + saleId);
                return ResponseEntity.ok(result);
            }

            PropertySalesDto sale = saleOpt.get();
            sale.setSaleStatus("LISTED");
            sale.setListingDate(LocalDateTime.now());
            sale.setActive(true);

            Optional<PropertySalesDto> updated = propertyService.updatePropertySale(saleId, sale);
            result.put("success", updated.isPresent());
            result.put("activated", updated.isPresent());
            result.put("sale_id", saleId.toString());
            if (updated.isEmpty()) result.put("error", "Failed to activate sale");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[activate-sale] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/activate-lease")
    @Operation(summary = "Activate a lease agreement (set status=ACTIVE, active=true)")
    public ResponseEntity<Map<String, Object>> activateLease(@RequestBody Map<String, Object> variables) {
        log.info("[activate-lease] Variables: {}", variables);
        Map<String, Object> result = new HashMap<>();

        try {
            Object leaseIdObj = variables.get("lease_id");
            if (leaseIdObj == null) {
                result.put("success", false);
                result.put("error", "Missing lease_id");
                return ResponseEntity.badRequest().body(result);
            }

            UUID leaseId = UUID.fromString(leaseIdObj.toString());
            Optional<PropertyLeaseDto> leaseOpt = propertyService.getPropertyLeaseById(leaseId);
            if (leaseOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "Lease not found: " + leaseId);
                return ResponseEntity.ok(result);
            }

            PropertyLeaseDto lease = leaseOpt.get();
            lease.setLeaseStatus("ACTIVE");
            lease.setActive(true);

            Optional<PropertyLeaseDto> updated = propertyService.updatePropertyLease(leaseId, lease);
            result.put("success", updated.isPresent());
            result.put("activated", updated.isPresent());
            result.put("lease_id", leaseId.toString());
            if (updated.isEmpty()) result.put("error", "Failed to activate lease");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[activate-lease] Error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

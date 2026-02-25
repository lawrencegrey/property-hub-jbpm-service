package com.greysoft.jbpm_engine.dto.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySalesDto {
    private UUID id;
    private UUID propertyId;
    private BigDecimal askingPrice;
    private String currency;
    private String saleStatus;
    private boolean active;
    private LocalDateTime listingDate;
    private LocalDateTime soldDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

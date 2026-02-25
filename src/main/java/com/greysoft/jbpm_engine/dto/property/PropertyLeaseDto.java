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
public class PropertyLeaseDto {
    private UUID id;
    private UUID propertyId;
    private BigDecimal leaseAmount;
    private String currency;
    private String leaseStatus;
    private boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

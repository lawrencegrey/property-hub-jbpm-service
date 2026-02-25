package com.greysoft.jbpm_engine.dto.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementDto {
    private UUID id;
    private UUID propertyId;
    private UUID offerId;
    private String transactionType;
    private String propertyType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

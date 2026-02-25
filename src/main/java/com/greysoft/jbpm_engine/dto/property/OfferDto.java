package com.greysoft.jbpm_engine.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {
    private UUID id;
    private UUID propertyId;
    private UUID personId;
    private BigDecimal offerAmount;
    private String currency;
    private boolean loanFinanced;
    private boolean usePlatform;
    private boolean accepted;
    private boolean active;
    private String processInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

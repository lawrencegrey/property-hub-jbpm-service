package com.greysoft.jbpm_engine.dto.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementPartyDto {
    private UUID id;
    private UUID engagementId;
    private UUID propertyId;
    private UUID personId;
    private String personType;
    private String represents;
    private boolean accepted;
    private boolean active;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

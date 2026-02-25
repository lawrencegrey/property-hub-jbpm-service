package com.greysoft.jbpm_engine.dto.people;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private UUID id;
    private UUID personId;
    private UUID documentId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String parish;
    private String country;
    private String postalCode;
    private boolean verified;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

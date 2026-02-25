package com.greysoft.jbpm_engine.dto.people;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonRoleDto {
    private UUID id;
    private UUID personId;
    private UUID documentId;
    private UUID addressId;
    private String roleType;
    private String profession;
    private String licenseNumber;
    private boolean verified;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

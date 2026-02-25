package com.greysoft.jbpm_engine.dto.people;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto {
    private UUID id;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String deviceToken;
    private String personType;
    private String trn;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String occupation;
    private String profilePictureUrl;
    private String status;
    private boolean verified;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

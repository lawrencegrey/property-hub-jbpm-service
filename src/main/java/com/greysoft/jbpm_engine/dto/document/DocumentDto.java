package com.greysoft.jbpm_engine.dto.document;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private UUID id;
    private UUID personId;
    private UUID propertyId;
    private String documentType;
    private String documentCategory;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageUrl;
    private String status;
    private String verificationStatus;
    private String verificationMethod;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private String issuer;
    private String issuerType;
    private String documentNumber;
    private LocalDateTime issueDate;
    private LocalDateTime expiryDate;
    private String countryOfIssue;
    private String description;
    private JsonNode metadata;
    private JsonNode verificationData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

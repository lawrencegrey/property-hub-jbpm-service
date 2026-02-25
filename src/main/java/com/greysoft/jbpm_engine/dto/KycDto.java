package com.greysoft.jbpm_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycDto {
    private UUID id;
    private UUID personId;

    private boolean idVerified;
    private LocalDateTime idVerificationDate;
    private UUID idDocumentId;

    private boolean passportPicVerified;
    private LocalDateTime passportPicVerificationDate;
    private UUID passportPicDocumentId;

    private boolean addressVerified;
    private LocalDateTime addressVerificationDate;
    private UUID addressDocumentId;

    private boolean trnVerified;
    private LocalDateTime trnVerificationDate;
    private UUID trnDocumentId;

    private boolean livelinessVerified;
    private LocalDateTime livelinessVerificationDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

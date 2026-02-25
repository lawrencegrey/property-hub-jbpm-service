package com.greysoft.jbpm_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDto {
    private String id;
    private String name;
    private String status;
    private String actualOwner;
    private String processInstanceId;
    private String processId;
    private String description;
    private String createdOn;
    private String activationTime;
    private Integer priority;
}

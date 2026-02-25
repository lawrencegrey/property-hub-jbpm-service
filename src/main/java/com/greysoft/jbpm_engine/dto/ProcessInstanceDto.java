package com.greysoft.jbpm_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceDto {
    private String processInstanceKey;
    private String processDefinitionKey;
    private String bpmnProcessId;
    private Integer version;
    private String state;
    private ZonedDateTime startTime;
    private Map<String, Object> variables;
}

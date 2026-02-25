package com.greysoft.jbpm_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionDto {
    private Long key;
    private String bpmnProcessId;
    private String name;
    private int version;
}

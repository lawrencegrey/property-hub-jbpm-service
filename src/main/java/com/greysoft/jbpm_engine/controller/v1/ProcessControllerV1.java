package com.greysoft.jbpm_engine.controller.v1;

import com.greysoft.jbpm_engine.dto.ProcessDefinitionDto;
import com.greysoft.jbpm_engine.dto.ProcessInstanceDto;
import com.greysoft.jbpm_engine.service.KieServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
@Tag(name = "Process Management V1", description = "APIs for interacting with jBPM workflow processes via KIE Server")
public class ProcessControllerV1 {

    private final KieServerService kieServerService;

    @PostMapping("/{processId}/start")
    @Operation(summary = "Start a process instance", description = "Start a new instance of a deployed process with the given process ID and variables", responses = {
            @ApiResponse(responseCode = "201", description = "Process instance started successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessInstanceDto.class))),
            @ApiResponse(responseCode = "404", description = "Process definition not found")
    })
    public ResponseEntity<ProcessInstanceDto> startProcess(
            @Parameter(description = "jBPM process ID", required = true) @PathVariable String processId,
            @Parameter(description = "Process variables", required = true) @RequestBody Map<String, Object> variables) {
        try {
            ProcessInstanceDto processInstance = kieServerService.startProcess(processId, variables);
            return ResponseEntity.status(HttpStatus.CREATED).body(processInstance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ProcessInstanceDto.builder().bpmnProcessId(processId).build());
        }
    }

    @DeleteMapping("/{processInstanceId}")
    @Operation(summary = "Cancel a process instance", description = "Cancel/abort a running process instance", responses = {
            @ApiResponse(responseCode = "204", description = "Process instance canceled successfully"),
            @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    public ResponseEntity<Void> cancelProcess(
            @Parameter(description = "Process instance ID", required = true) @PathVariable String processInstanceId) {
        try {
            kieServerService.cancelProcessInstance(processInstanceId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/definitions")
    @Operation(summary = "List all process definitions", description = "Returns all process definitions from jBPM KIE Server")
    public ResponseEntity<List<ProcessDefinitionDto>> getProcessDefinitions() {
        List<ProcessDefinitionDto> definitions = kieServerService.getProcessDefinitions();
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/instances/{id}")
    @Operation(summary = "Get process instance by ID", description = "Returns a single process instance by its ID")
    public ResponseEntity<ProcessInstanceDto> getProcessInstance(
            @Parameter(description = "Process instance ID", required = true) @PathVariable String id) {
        try {
            ProcessInstanceDto instance = kieServerService.getProcessInstance(id);
            if (instance == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/instances/{id}/variables")
    @Operation(summary = "Get process instance variables", description = "Returns all variables for a specific process instance")
    public ResponseEntity<Map<String, Object>> getProcessInstanceVariables(
            @Parameter(description = "Process instance ID", required = true) @PathVariable String id) {
        try {
            Map<String, Object> variables = kieServerService.getProcessInstanceVariables(id);
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/instances/{id}/signal/{signalName}")
    @Operation(summary = "Send signal to process instance", description = "Send a signal to a running process instance")
    public ResponseEntity<Void> signalProcessInstance(
            @Parameter(description = "Process instance ID", required = true) @PathVariable String id,
            @Parameter(description = "Signal name", required = true) @PathVariable String signalName,
            @RequestBody(required = false) Map<String, Object> data) {
        try {
            kieServerService.signalProcessInstance(id, signalName, data);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{processId}/instances/batch")
    @Operation(summary = "Start multiple process instances", description = "Start multiple instances of a process with different variables")
    public ResponseEntity<List<ProcessInstanceDto>> startProcessBatch(
            @Parameter(description = "jBPM process ID", required = true) @PathVariable String processId,
            @Parameter(description = "List of process variables for each instance", required = true) @RequestBody List<Map<String, Object>> variablesList) {
        try {
            List<ProcessInstanceDto> instances = variablesList.stream()
                    .map(variables -> {
                        try {
                            return kieServerService.startProcess(processId, variables);
                        } catch (Exception e) {
                            return ProcessInstanceDto.builder()
                                    .bpmnProcessId(processId)
                                    .build();
                        }
                    })
                    .toList();
            return ResponseEntity.status(HttpStatus.CREATED).body(instances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check for Process Management V1")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "version", "v1",
                "service", "jBPM Process Management",
                "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(health);
    }
}

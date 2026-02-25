package com.greysoft.jbpm_engine.service;

import com.greysoft.jbpm_engine.config.KieServerConfig;
import com.greysoft.jbpm_engine.dto.ProcessDefinitionDto;
import com.greysoft.jbpm_engine.dto.ProcessInstanceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core jBPM service — replaces ZeebeService + ProcessService from the Camunda workflow-engine.
 * Communicates with jBPM KIE Server REST API for process and task operations.
 */
@Service
@Slf4j
public class KieServerService {

    private final WebClient kieServerWebClient;
    private final KieServerConfig kieServerConfig;

    // Mapping from Camunda-style task states to jBPM task states
    private static final Map<String, String> STATE_MAPPING = Map.of(
            "CREATED", "Ready,Reserved",
            "COMPLETED", "Completed",
            "CANCELED", "Exited",
            "FAILED", "Failed",
            "IN_PROGRESS", "InProgress",
            "RESERVED", "Reserved"
    );

    public KieServerService(WebClient kieServerWebClient, KieServerConfig kieServerConfig) {
        this.kieServerWebClient = kieServerWebClient;
        this.kieServerConfig = kieServerConfig;
    }

    /**
     * Translate a Camunda-style state (e.g., CREATED) to a jBPM status (e.g., Ready).
     * If the state is already a valid jBPM status, it is returned as-is.
     */
    private String mapToJbpmStatus(String state) {
        if (state == null || state.isEmpty()) return state;
        String mapped = STATE_MAPPING.get(state.toUpperCase());
        return mapped != null ? mapped : state; // pass through if already jBPM-style
    }

    // ========================================
    // Process Definitions
    // ========================================

    @SuppressWarnings("unchecked")
    public List<ProcessDefinitionDto> getProcessDefinitions() {
        try {
            log.info("Fetching process definitions from KIE Server");
            Map<String, Object> response = kieServerWebClient.get()
                    .uri("/containers/{containerId}/processes", kieServerConfig.getContainerId())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return Collections.emptyList();

            Object processes = response.get("processes");
            if (!(processes instanceof List<?>)) return Collections.emptyList();

            return ((List<Map<String, Object>>) processes).stream()
                    .map(p -> ProcessDefinitionDto.builder()
                            .key(getLong(p, "process-id-kbase"))
                            .bpmnProcessId((String) p.get("process-id"))
                            .name((String) p.get("process-name"))
                            .version(getInt(p, "process-version"))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching process definitions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch process definitions", e);
        }
    }

    // ========================================
    // Process Instance Operations
    // ========================================

    @SuppressWarnings("unchecked")
    public ProcessInstanceDto startProcess(String processId, Map<String, Object> variables) {
        try {
            log.info("Starting process '{}' with variables: {}", processId, variables);
            Long instanceId = kieServerWebClient.post()
                    .uri("/containers/{containerId}/processes/{processId}/instances",
                            kieServerConfig.getContainerId(), processId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(variables != null ? variables : Collections.emptyMap())
                    .retrieve()
                    .bodyToMono(Long.class)
                    .block();

            log.info("Started process instance: {}", instanceId);

            return ProcessInstanceDto.builder()
                    .bpmnProcessId(processId)
                    .processInstanceKey(String.valueOf(instanceId))
                    .variables(variables)
                    .build();

        } catch (Exception e) {
            log.error("Error starting process instance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start process instance", e);
        }
    }

    public void cancelProcessInstance(String processInstanceId) {
        try {
            log.info("Canceling process instance: {}", processInstanceId);
            kieServerWebClient.delete()
                    .uri("/containers/{containerId}/processes/instances/{instanceId}",
                            kieServerConfig.getContainerId(), processInstanceId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Canceled process instance: {}", processInstanceId);

        } catch (Exception e) {
            log.error("Error canceling process instance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cancel process instance", e);
        }
    }

    @SuppressWarnings("unchecked")
    public ProcessInstanceDto getProcessInstance(String processInstanceId) {
        try {
            Map<String, Object> response = kieServerWebClient.get()
                    .uri("/containers/{containerId}/processes/instances/{instanceId}",
                            kieServerConfig.getContainerId(), processInstanceId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return null;

            return ProcessInstanceDto.builder()
                    .processInstanceKey(String.valueOf(response.get("process-instance-id")))
                    .bpmnProcessId((String) response.get("process-id"))
                    .state(mapJbpmState(getInt(response, "process-instance-state")))
                    .build();

        } catch (Exception e) {
            log.error("Error fetching process instance {}: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch process instance", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProcessInstanceVariables(String processInstanceId) {
        try {
            log.info("Fetching variables for process instance: {}", processInstanceId);
            Map<String, Object> variables = kieServerWebClient.get()
                    .uri("/containers/{containerId}/processes/instances/{instanceId}/variables",
                            kieServerConfig.getContainerId(), processInstanceId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return variables != null ? variables : Collections.emptyMap();

        } catch (Exception e) {
            log.error("Error fetching variables: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch process instance variables", e);
        }
    }

    // ========================================
    // Task Operations
    // ========================================

    public void completeTask(String taskId, Map<String, Object> variables) {
        try {
            log.info("Completing task {} with variables: {}", taskId, variables);
            kieServerWebClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/containers/{containerId}/tasks/{taskId}/states/completed")
                            .queryParam("user", "wbadmin")
                            .queryParam("auto-progress", true)
                            .build(kieServerConfig.getContainerId(), taskId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(variables != null ? variables : Collections.emptyMap())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Successfully completed task {}", taskId);

        } catch (Exception e) {
            log.error("Error completing task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete task: " + e.getMessage(), e);
        }
    }

    public void assignTask(String taskId, String userId) {
        try {
            log.info("Assigning task {} to user '{}'", taskId, userId);
            kieServerWebClient.put()
                    .uri("/containers/{containerId}/tasks/{taskId}/states/delegated?targetUser={userId}",
                            kieServerConfig.getContainerId(), taskId, userId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Task {} successfully assigned to {}", taskId, userId);

        } catch (Exception e) {
            log.error("Failed to assign task {}: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("Error assigning task: " + e.getMessage(), e);
        }
    }

    public void claimTask(String taskId, String userId) {
        try {
            log.info("Claiming/assigning task {} for user '{}' (treating as no-op for external user IDs)", taskId, userId);
            // In jBPM, tasks with a single potentialOwner are auto-Reserved to that owner.
            // External person IDs (UUIDs from person-service) don't exist in jBPM's user system,
            // so we log the assignment intent but don't call the KIE Server claim endpoint.
            // The task remains assigned to the process owner (wbadmin) who acts on behalf of the user.
            log.info("Task {} assignment recorded for external user {} (task remains with process owner)", taskId, userId);

        } catch (Exception e) {
            log.error("Failed to claim task {}: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("Error claiming task: " + e.getMessage(), e);
        }
    }

    public void startTask(String taskId, String userId) {
        try {
            log.info("Starting task {} for user '{}'", taskId, userId);
            kieServerWebClient.put()
                    .uri("/containers/{containerId}/tasks/{taskId}/states/started?user={userId}",
                            kieServerConfig.getContainerId(), taskId, userId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Task {} successfully started by {}", taskId, userId);

        } catch (Exception e) {
            log.error("Failed to start task {}: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("Error starting task: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTasksByProcessInstance(String processInstanceId, String status) {
        try {
            status = mapToJbpmStatus(status);
            log.info("Getting tasks for process instance {} with status {}", processInstanceId, status);

            // Build URI with multiple status params if comma-separated (e.g., "Ready,Reserved")
            StringBuilder uriBuilder = new StringBuilder("/queries/tasks/instances/pot-owners?processInstanceId=")
                    .append(processInstanceId);
            if (status != null && !status.isEmpty()) {
                for (String s : status.split(",")) {
                    uriBuilder.append("&status=").append(s.trim());
                }
            }

            Map<String, Object> response = kieServerWebClient.get()
                    .uri(uriBuilder.toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return Collections.emptyList();

            Object taskSummary = response.get("task-summary");
            if (!(taskSummary instanceof List<?>)) return Collections.emptyList();

            return (List<Map<String, Object>>) taskSummary;

        } catch (Exception e) {
            log.error("Error getting tasks for process instance {}: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException("Failed to get tasks", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTasksByUser(String userId, String status) {
        try {
            status = mapToJbpmStatus(status);
            log.info("Getting tasks for user {} with status {}", userId, status);

            Map<String, Object> response = kieServerWebClient.get()
                    .uri("/queries/tasks/instances/owners?user={userId}&status={status}", userId, status)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return Collections.emptyList();

            Object taskSummary = response.get("task-summary");
            if (!(taskSummary instanceof List<?>)) return Collections.emptyList();

            return (List<Map<String, Object>>) taskSummary;

        } catch (Exception e) {
            log.error("Error getting tasks for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user tasks", e);
        }
    }

    // ========================================
    // Signal & Abort
    // ========================================

    public void signalProcessInstance(String processInstanceId, String signalName, Map<String, Object> data) {
        try {
            log.info("Sending signal '{}' to process instance {}", signalName, processInstanceId);
            kieServerWebClient.post()
                    .uri("/containers/{containerId}/processes/instances/{instanceId}/signal/{signalName}",
                            kieServerConfig.getContainerId(), processInstanceId, signalName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(data != null ? data : Collections.emptyMap())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Signal '{}' sent to process instance {}", signalName, processInstanceId);

        } catch (Exception e) {
            log.error("Error sending signal: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send signal", e);
        }
    }

    // ========================================
    // Health Check
    // ========================================

    public boolean isServiceUp() {
        try {
            kieServerWebClient.get()
                    .uri("")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("[isServiceUp] KIE Server health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ========================================
    // Helpers
    // ========================================

    private String mapJbpmState(int state) {
        return switch (state) {
            case 0 -> "PENDING";
            case 1 -> "ACTIVE";
            case 2 -> "COMPLETED";
            case 3 -> "ABORTED";
            case 4 -> "SUSPENDED";
            default -> "UNKNOWN";
        };
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
}

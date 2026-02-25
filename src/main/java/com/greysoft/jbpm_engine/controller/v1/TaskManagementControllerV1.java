package com.greysoft.jbpm_engine.controller.v1;

import com.greysoft.jbpm_engine.dto.TaskResponseDto;
import com.greysoft.jbpm_engine.service.KieServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management V1", description = "APIs for managing user tasks in jBPM workflow processes")
public class TaskManagementControllerV1 {

    private final KieServerService kieServerService;

    @PatchMapping("/{taskId}/assign/{personId}")
    @Operation(summary = "Assign a task to a user", description = "Claim and assign a task to a user via KIE Server", responses = {
            @ApiResponse(responseCode = "200", description = "Task assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Boolean> assignTask(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId,
            @Parameter(description = "Person ID to assign", required = true) @PathVariable String personId) {
        try {
            kieServerService.claimTask(taskId, personId);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{taskId}/complete")
    @Operation(summary = "Complete a task", description = "Complete a user task with optional variables", responses = {
            @ApiResponse(responseCode = "200", description = "Task completed successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Boolean> completeTask(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> requestBody) {

        log.info("Completing task: {}", taskId);

        try {
            Map<String, Object> variables = Map.of();
            if (requestBody != null && requestBody.containsKey("variables")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> variablesList = (List<Map<String, Object>>) requestBody.get("variables");
                variables = variablesList.stream()
                        .collect(Collectors.toMap(
                                var -> (String) var.get("name"),
                                var -> var.get("value")
                        ));
            }

            kieServerService.completeTask(taskId, variables);
            log.info("Task {} completed successfully", taskId);
            return ResponseEntity.ok(true);

        } catch (Exception e) {
            log.error("Error completing task {}: {}", taskId, e.getMessage(), e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's tasks by state", description = "Retrieve tasks assigned to a specific user filtered by task state")
    public ResponseEntity<List<TaskResponseDto>> getUserTasksByState(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @Parameter(description = "Task state filter (e.g., Ready, Reserved, InProgress, Completed)", required = true) @RequestParam String state) {

        List<Map<String, Object>> rawTasks = kieServerService.getTasksByUser(userId, state);
        List<TaskResponseDto> tasks = rawTasks.stream()
                .map(this::mapToTaskResponse)
                .toList();

        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(tasks);
        }
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/process/instance/{processInstanceId}")
    @Operation(summary = "Get tasks by process instance ID and state", description = "Retrieve all tasks filtered by process instance ID and state")
    public ResponseEntity<List<TaskResponseDto>> getTasksByProcessInstance(
            @Parameter(description = "Process Instance ID", required = true) @PathVariable String processInstanceId,
            @Parameter(description = "Task State (e.g., Ready, Reserved, InProgress, Completed)", required = true) @RequestParam String state) {

        List<Map<String, Object>> rawTasks = kieServerService.getTasksByProcessInstance(processInstanceId, state);
        List<TaskResponseDto> tasks = rawTasks.stream()
                .map(this::mapToTaskResponse)
                .toList();

        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(tasks);
        }
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/{taskId}/start")
    @Operation(summary = "Start a task", description = "Transition a task to InProgress state")
    public ResponseEntity<Boolean> startTask(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId,
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            kieServerService.startTask(taskId, userId);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{taskId}/reassign")
    @Operation(summary = "Reassign a task to another user")
    public ResponseEntity<Boolean> reassignTask(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId,
            @RequestBody Map<String, String> request) {
        try {
            String newUserId = request.get("userId");
            if (newUserId == null || newUserId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            kieServerService.assignTask(taskId, newUserId);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check for Task Management V1")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "version", "v1",
                "service", "jBPM Task Management",
                "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(health);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get task summary statistics")
    public ResponseEntity<Map<String, Object>> getTaskSummary() {
        try {
            Map<String, Object> summary = Map.of(
                    "totalTasks", 0,
                    "activeTasks", 0,
                    "completedTasks", 0,
                    "failedTasks", 0,
                    "lastUpdated", java.time.Instant.now().toString()
            );
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private TaskResponseDto mapToTaskResponse(Map<String, Object> raw) {
        TaskResponseDto dto = new TaskResponseDto();
        dto.setId(String.valueOf(raw.get("task-id")));
        dto.setName((String) raw.get("task-name"));
        dto.setStatus((String) raw.get("task-status"));
        dto.setActualOwner((String) raw.get("task-actual-owner"));
        dto.setProcessInstanceId(String.valueOf(raw.get("task-proc-inst-id")));
        dto.setProcessId((String) raw.get("task-proc-def-id"));
        return dto;
    }
}

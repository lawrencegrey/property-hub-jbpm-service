package com.greysoft.jbpm_engine.controller.v1.worker;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic callback endpoint for jBPM service task execution.
 * <p>
 * The KJAR's {@code RestCallbackWorkItemHandler} POSTs to this endpoint
 * with {@code {"taskName": "...", "parameters": {...}}}. This controller
 * looks up the matching handler from the {@code workItemHandlerRegistry}
 * and dispatches the call, returning the results as JSON.
 */
@RestController
@RequestMapping("/api/internal/workitem")
@Slf4j
public class WorkItemCallbackController {

    private final Map<String, WorkItemHandler> registry;

    public WorkItemCallbackController(
            @Qualifier("workItemHandlerRegistry") Map<String, WorkItemHandler> registry) {
        this.registry = registry;
        log.info("[WorkItemCallback] Initialized with {} registered handlers", registry.size());
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeWorkItem(
            @RequestBody Map<String, Object> request) {

        String taskName = (String) request.get("taskName");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.getOrDefault("parameters", Map.of());

        log.info("[WorkItemCallback] Received callback for taskName={}", taskName);

        if (taskName == null || taskName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing taskName"));
        }

        WorkItemHandler handler = registry.get(taskName);
        if (handler == null) {
            log.warn("[WorkItemCallback] No handler registered for taskName={}", taskName);
            // Return success anyway — unhandled tasks still need to complete
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "_unhandled", true,
                    "_message", "No handler for: " + taskName));
        }

        try {
            // Create a lightweight WorkItem adapter and capture results
            CallbackWorkItem workItem = new CallbackWorkItem(taskName, parameters);
            AtomicReference<Map<String, Object>> capturedResults = new AtomicReference<>(new HashMap<>());

            // Create a WorkItemManager that captures the completion results
            WorkItemManager capturingManager = new WorkItemManager() {
                @Override
                public void completeWorkItem(long id, Map<String, Object> results) {
                    capturedResults.set(results != null ? results : new HashMap<>());
                }

                @Override
                public void abortWorkItem(long id) {
                    capturedResults.set(Map.of("success", false, "error", "Work item aborted"));
                }

                @Override
                public void registerWorkItemHandler(String name, WorkItemHandler handler) {
                    // no-op
                }
            };

            handler.executeWorkItem(workItem, capturingManager);

            Map<String, Object> results = capturedResults.get();
            log.info("[WorkItemCallback] taskName={} completed with keys: {}", taskName, results.keySet());
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("[WorkItemCallback] Error executing taskName={}: {}", taskName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Lightweight WorkItem adapter that wraps the callback parameters.
     */
    private static class CallbackWorkItem implements WorkItem {
        private final String name;
        private final Map<String, Object> parameters;

        CallbackWorkItem(String name, Map<String, Object> parameters) {
            this.name = name;
            this.parameters = parameters != null ? parameters : Map.of();
        }

        @Override public long getId() { return 0; }
        @Override public String getName() { return name; }
        @Override public int getState() { return 0; }
        @Override public Map<String, Object> getParameters() { return parameters; }
        @Override public Object getParameter(String name) { return parameters.get(name); }
        @Override public Object getResult(String name) { return null; }
        @Override public Map<String, Object> getResults() { return new HashMap<>(); }
        @Override public long getProcessInstanceId() { return 0; }

        // Required by interface but not needed for callback dispatch
        public String getDeploymentId() { return ""; }
        public long getNodeInstanceId() { return 0; }
        public long getNodeId() { return 0; }
    }
}

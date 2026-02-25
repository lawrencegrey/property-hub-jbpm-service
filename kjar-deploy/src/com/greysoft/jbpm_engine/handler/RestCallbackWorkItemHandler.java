package com.greysoft.jbpm_engine.handler;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic REST callback work item handler for jBPM KJAR.
 * <p>
 * When a BPMN service task executes, this handler POSTs the task name and all
 * parameters as JSON to the property-workflow-wrapper's callback endpoint.
 * The wrapper dispatches to the correct service handler and returns results.
 * <p>
 * Always completes the work item (even on error) to avoid blocking the process.
 */
public class RestCallbackWorkItemHandler implements WorkItemHandler {

    private static final Logger LOG = Logger.getLogger(RestCallbackWorkItemHandler.class.getName());

    private final String callbackUrl;

    /** Default constructor — uses Docker network hostname. */
    public RestCallbackWorkItemHandler() {
        this("http://property-workflow-wrapper:8006/api/internal/workitem/execute");
    }

    /** Parameterized constructor for custom callback URL. */
    public RestCallbackWorkItemHandler(String callbackUrl) {
        this.callbackUrl = callbackUrl;
        LOG.info("[RestCallback] Initialized with callbackUrl=" + callbackUrl);
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskName = workItem.getName();
        Map<String, Object> params = workItem.getParameters();

        LOG.info("[RestCallback] Executing taskName=" + taskName + ", workItemId=" + workItem.getId());

        Map<String, Object> results = new HashMap<>();

        try {
            // Build JSON payload: {"taskName": "xxx", "parameters": {...}}
            String json = buildJson(taskName, params);

            // HTTP POST to the wrapper's callback endpoint
            URL url = new URL(callbackUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = readFully(stream);
            conn.disconnect();

            if (code >= 200 && code < 300) {
                // Parse response JSON into results map
                results = parseJsonFlat(responseBody);
                LOG.info("[RestCallback] taskName=" + taskName + " succeeded, results keys: " + results.keySet());
            } else {
                LOG.warning("[RestCallback] taskName=" + taskName + " returned HTTP " + code + ": " + responseBody);
                results.put("_callback_success", "false");
                results.put("_callback_error", "HTTP " + code + ": " + responseBody);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RestCallback] taskName=" + taskName + " error: " + e.getMessage(), e);
            results.put("_callback_success", "false");
            results.put("_callback_error", e.getMessage());
        }

        // Always complete to not block the process
        manager.completeWorkItem(workItem.getId(), results);
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        LOG.warning("[RestCallback] Aborting workItemId=" + workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }

    // ===== JSON helpers (no external dependencies) =====

    private String buildJson(String taskName, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"taskName\":\"").append(escapeJson(taskName)).append("\",\"parameters\":{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(escapeJson(entry.getKey())).append("\":");
            appendJsonValue(sb, entry.getValue());
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }

    private void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else {
            sb.append('"').append(escapeJson(value.toString())).append('"');
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String readFully(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /**
     * Simple flat JSON parser for the response {"key":"value", "key2": 123, ...}.
     * Handles strings, numbers, booleans, and null at the top level only.
     */
    private Map<String, Object> parseJsonFlat(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;

        // Strip outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        int len = json.length();
        while (i < len) {
            // Skip whitespace and commas
            while (i < len && (json.charAt(i) == ' ' || json.charAt(i) == ',' || json.charAt(i) == '\n'))
                i++;
            if (i >= len) break;

            // Read key
            if (json.charAt(i) != '"') { i++; continue; }
            int keyStart = ++i;
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // skip escaped char
                i++;
            }
            String key = json.substring(keyStart, i);
            i++; // skip closing quote

            // Skip colon and whitespace
            while (i < len && (json.charAt(i) == ':' || json.charAt(i) == ' ')) i++;
            if (i >= len) break;

            // Read value
            char c = json.charAt(i);
            if (c == '"') {
                // String value
                int valStart = ++i;
                StringBuilder val = new StringBuilder();
                while (i < len && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < len) {
                        i++;
                        switch (json.charAt(i)) {
                            case 'n': val.append('\n'); break;
                            case 'r': val.append('\r'); break;
                            case 't': val.append('\t'); break;
                            case '"': val.append('"'); break;
                            case '\\': val.append('\\'); break;
                            default: val.append(json.charAt(i));
                        }
                    } else {
                        val.append(json.charAt(i));
                    }
                    i++;
                }
                i++; // skip closing quote
                map.put(key, val.toString());
            } else if (c == 't' || c == 'f') {
                // Boolean
                if (json.startsWith("true", i)) {
                    map.put(key, "true");
                    i += 4;
                } else {
                    map.put(key, "false");
                    i += 5;
                }
            } else if (c == 'n') {
                // null
                map.put(key, null);
                i += 4;
            } else if (c == '-' || Character.isDigit(c)) {
                // Number
                int numStart = i;
                while (i < len && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.' || json.charAt(i) == '-'))
                    i++;
                map.put(key, json.substring(numStart, i));
            } else if (c == '{' || c == '[') {
                // Nested object/array — store as raw string (skip it)
                int depth = 1;
                char open = c, close = (c == '{') ? '}' : ']';
                int start = i;
                i++;
                while (i < len && depth > 0) {
                    if (json.charAt(i) == open) depth++;
                    else if (json.charAt(i) == close) depth--;
                    else if (json.charAt(i) == '"') {
                        i++;
                        while (i < len && json.charAt(i) != '"') {
                            if (json.charAt(i) == '\\') i++;
                            i++;
                        }
                    }
                    i++;
                }
                map.put(key, json.substring(start, i));
            } else {
                i++;
            }
        }
        return map;
    }
}

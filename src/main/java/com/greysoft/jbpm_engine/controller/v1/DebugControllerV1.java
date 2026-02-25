package com.greysoft.jbpm_engine.controller.v1;

import com.greysoft.jbpm_engine.dto.document.DocumentDto;
import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.property.PropertyDto;
import com.greysoft.jbpm_engine.service.AuthService;
import com.greysoft.jbpm_engine.service.DocumentService;
import com.greysoft.jbpm_engine.service.PersonService;
import com.greysoft.jbpm_engine.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Debug V1", description = "Debug endpoints for testing authentication and service calls")
public class DebugControllerV1 {

    private final DocumentService documentService;
    private final PersonService personService;
    private final PropertyService propertyService;
    private final AuthService authService;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @GetMapping("/health")
    @Operation(summary = "Health check for Debug API")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "Debug API V1 (jBPM Engine)",
            "keycloakTokenUri", keycloakTokenUri
        );
        return ResponseEntity.ok(health);
    }

    @GetMapping("/token")
    @Operation(summary = "Get access token for backend services")
    public ResponseEntity<Map<String, Object>> getToken() {
        try {
            String token = authService.getAccessToken();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tokenLength", token.length());
            response.put("tokenPrefix", token.substring(0, Math.min(50, token.length())));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/document/{id}")
    @Operation(summary = "Test document fetch by ID")
    public ResponseEntity<Map<String, Object>> testDocumentFetch(
            @Parameter(description = "Document ID") @PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", id);
        try {
            UUID documentUuid = UUID.fromString(id);
            Optional<DocumentDto> document = documentService.getDocumentById(documentUuid);
            response.put("success", document.isPresent());
            response.put("documentFound", document.isPresent());
            document.ifPresent(d -> response.put("document", d));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/person/{id}")
    @Operation(summary = "Test person fetch by ID")
    public ResponseEntity<Map<String, Object>> testPersonFetch(
            @Parameter(description = "Person UUID") @PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("personId", id);
        try {
            UUID personUuid = UUID.fromString(id);
            Optional<PersonDto> person = personService.getPersonById(personUuid);
            response.put("success", person.isPresent());
            response.put("personFound", person.isPresent());
            person.ifPresent(p -> response.put("person", p));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/property/{id}")
    @Operation(summary = "Test property fetch by ID")
    public ResponseEntity<Map<String, Object>> testPropertyFetch(
            @Parameter(description = "Property UUID") @PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("propertyId", id);
        try {
            UUID propertyUuid = UUID.fromString(id);
            Optional<PropertyDto> property = propertyService.getPropertyById(propertyUuid);
            response.put("success", property.isPresent());
            response.put("propertyFound", property.isPresent());
            property.ifPresent(p -> response.put("property", p));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

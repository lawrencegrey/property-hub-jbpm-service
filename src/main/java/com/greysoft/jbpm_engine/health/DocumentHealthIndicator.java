package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.DocumentService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DocumentHealthIndicator implements HealthIndicator {
    private final DocumentService documentService;

    public DocumentHealthIndicator(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public Health health() {
        if (documentService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "Document API unreachable").build();
        }
    }
}

package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.VerificationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class VerificationHealthIndicator implements HealthIndicator {
    private final VerificationService verificationService;

    public VerificationHealthIndicator(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public Health health() {
        if (verificationService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "Verification API unreachable").build();
        }
    }
}

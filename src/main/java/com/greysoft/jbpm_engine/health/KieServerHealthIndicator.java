package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.KieServerService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class KieServerHealthIndicator implements HealthIndicator {
    private final KieServerService kieServerService;

    public KieServerHealthIndicator(KieServerService kieServerService) {
        this.kieServerService = kieServerService;
    }

    @Override
    public Health health() {
        if (kieServerService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "KIE Server unreachable").build();
        }
    }
}

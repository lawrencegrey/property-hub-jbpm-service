package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.PropertyService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PropertyHealthIndicator implements HealthIndicator {
    private final PropertyService propertyService;

    public PropertyHealthIndicator(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Health health() {
        if (propertyService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "Property API unreachable").build();
        }
    }
}

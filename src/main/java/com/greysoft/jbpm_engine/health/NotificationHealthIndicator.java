package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.NotificationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class NotificationHealthIndicator implements HealthIndicator {
    private final NotificationService notificationService;

    public NotificationHealthIndicator(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public Health health() {
        if (notificationService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "Notification API unreachable").build();
        }
    }
}

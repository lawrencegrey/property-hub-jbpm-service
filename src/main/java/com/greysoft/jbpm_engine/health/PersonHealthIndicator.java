package com.greysoft.jbpm_engine.health;

import com.greysoft.jbpm_engine.service.PersonService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PersonHealthIndicator implements HealthIndicator {
    private final PersonService personService;

    public PersonHealthIndicator(PersonService personService) {
        this.personService = personService;
    }

    @Override
    public Health health() {
        if (personService.isServiceUp()) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("error", "Person API unreachable").build();
        }
    }
}

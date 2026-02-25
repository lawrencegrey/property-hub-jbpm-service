package com.greysoft.jbpm_engine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for connecting to jBPM KIE Server REST API.
 * Provides an authenticated WebClient pre-configured with Basic Auth.
 */
@Configuration
public class KieServerConfig {

    @Value("${jbpm.kieserver.url}")
    private String kieServerUrl;

    @Value("${jbpm.kieserver.user}")
    private String kieServerUser;

    @Value("${jbpm.kieserver.password}")
    private String kieServerPassword;

    @Value("${jbpm.container.id}")
    private String containerId;

    @Bean
    public WebClient kieServerWebClient() {
        return WebClient.builder()
                .baseUrl(kieServerUrl)
                .filter(ExchangeFilterFunctions.basicAuthentication(kieServerUser, kieServerPassword))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    public String getKieServerUrl() {
        return kieServerUrl;
    }

    public String getContainerId() {
        return containerId;
    }
}

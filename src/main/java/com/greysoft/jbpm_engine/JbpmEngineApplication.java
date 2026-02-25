package com.greysoft.jbpm_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "jBPM Engine API",
        version = "1.0",
        description = "API for interacting with jBPM 7 workflow engine via KIE Server REST API"
    )
)
public class JbpmEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(JbpmEngineApplication.class, args);
	}
}

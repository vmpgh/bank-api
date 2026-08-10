package com.bank.bankapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI bankApi() {

        return new OpenAPI()

                .info(new Info()

                        .title("Bank API")

                        .version("1.0")

                        .description("""
                    Enterprise-grade banking REST API demonstrating modern backend
                    engineering practices using Spring Boot 4.
            
                    Highlights:
                    • Secure JWT Authentication & Authorization
                    • RESTful Account and Transfer APIs
                    • Event-Driven Architecture with Kafka
                    • Transactional Outbox Pattern
                    • Redis Caching
                    • Optimistic Locking
                    • Flyway Schema Versioning
                    • OpenTelemetry Tracing & Prometheus Metrics
                    • Dockerized Deployment
                    • Testcontainers-powered Integration Testing
                    • Comprehensive Unit, Integration and End-to-End Testing
                    """)

                        .contact(new Contact()
                                .name("Project Maintainer")
                                .url("https://github.com/vmpgh"))
                );
    }
}
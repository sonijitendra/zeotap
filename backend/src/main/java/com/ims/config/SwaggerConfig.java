package com.ims.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI imsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mission-Critical Incident Management System API")
                        .description("Production-grade SRE incident management platform for signal ingestion, " +
                                "incident lifecycle management, and root cause analysis.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IMS Engineering Team")
                                .email("ims-eng@company.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("http://ims-backend:8080").description("Docker Environment")));
    }
}

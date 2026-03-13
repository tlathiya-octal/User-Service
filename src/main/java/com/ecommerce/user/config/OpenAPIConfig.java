package com.ecommerce.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .version("v1")
                        .description("User profile APIs for the e-commerce microservices platform")
                        .contact(new Contact().name("E-Commerce Platform").email("support@example.com")))
                .components(new Components().addSecuritySchemes(
                        securitySchemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}

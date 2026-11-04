package com.union.solutions.saascore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI(@Value("${app.version}") String appVersion) {
    return new OpenAPI()
        .info(
            new Info()
                .title("Spring SaaS Core API")
                .version(appVersion)
                .description(
                    "Multi-tenant SaaS governance core: tenants, ABAC policies, feature flags, audit")
                .contact(
                    new Contact()
                        .name("Union Solutions")
                        .email("contato@unionsolutions.tech")
                        .url("https://github.com/union-solutions"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}

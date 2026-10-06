package de.sedzkitchens.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Describes the API for the generated documentation at /swagger-ui.html and tells it that requests carry
// a login token, so the "Authorize" button there can be used with a token from /api/auth/login
@Configuration
@OpenAPIDefinition(
		info = @Info(title = "SedzKitchens API", version = "1.0",
				description = "Internal management system for a kitchen studio"),
		security = @SecurityRequirement(name = "bearer-token"))
@SecurityScheme(name = "bearer-token", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

}

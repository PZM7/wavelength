package com.wavelength.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Wavelength API")
                                .version("v1")
                                .description(
                                        "Modular music social API. See docs/api.md for PATCH and"
                                                + " privacy semantics."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer errors() {
        return api -> {
            var errorSchema =
                    new Schema<>()
                            .type("object")
                            .addProperty("code", new Schema<String>().type("string"))
                            .addProperty("message", new Schema<String>().type("string"))
                            .addProperty(
                                    "timestamp",
                                    new Schema<String>().type("string").format("date-time"))
                            .addProperty("path", new Schema<String>().type("string"));
            api.getComponents().addSchemas("ApiError", errorSchema);
            api.getPaths()
                    .forEach(
                            (path, item) ->
                                    item.readOperations()
                                            .forEach(
                                                    operation -> {
                                                        if (path.equals("/api/v1/health"))
                                                            operation.setSecurity(List.of());
                                                        else
                                                            Map.of(
                                                                            "400",
                                                                            "Invalid input",
                                                                            "401",
                                                                            "Invalid or missing"
                                                                                    + " JWT",
                                                                            "403",
                                                                            "Forbidden",
                                                                            "404",
                                                                            "Not found or not"
                                                                                    + " visible",
                                                                            "409",
                                                                            "Conflict",
                                                                            "429",
                                                                            "Rate limited by"
                                                                                    + " gateway",
                                                                            "500",
                                                                            "Internal error")
                                                                    .forEach(
                                                                            (code, description) ->
                                                                                    operation
                                                                                            .getResponses()
                                                                                            .addApiResponse(
                                                                                                    code,
                                                                                                    new ApiResponse()
                                                                                                            .description(
                                                                                                                    description)
                                                                                                            .content(
                                                                                                                    new Content()
                                                                                                                            .addMediaType(
                                                                                                                                    "application/json",
                                                                                                                                    new MediaType()
                                                                                                                                            .schema(
                                                                                                                                                    new Schema<>()
                                                                                                                                                            .$ref(
                                                                                                                                                                    "#/components/schemas/ApiError"))))));
                                                    }));
        };
    }
}

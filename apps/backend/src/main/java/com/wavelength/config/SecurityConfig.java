package com.wavelength.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.common.ApiError;
import com.wavelength.common.TextInput;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {
    private final ObjectMapper mapper;
    private final Environment environment;

    public SecurityConfig(ObjectMapper mapper, Environment environment) {
        this.mapper = mapper;
        this.environment = environment;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${wavelength.auth.issuer}") String issuer,
            @Value("${wavelength.auth.jwk-set-uri}") String jwks,
            @Value("${wavelength.auth.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> claims =
                jwt -> {
                    if (jwt.getAudience() != null
                            && jwt.getAudience().contains(audience)
                            && jwt.getSubject() != null
                            && !TextInput.isBlank(jwt.getSubject())
                            && jwt.getSubject().length() <= 255
                            && jwt.getExpiresAt() != null) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    "invalid_token", "Required claims missing or invalid", null));
                };
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer), claims));
        return decoder;
    }

    @Bean
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers(
                                            "/api/v1/health",
                                            "/actuator/health",
                                            "/actuator/health/**")
                                    .permitAll();
                            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                                auth.requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll();
                            }
                            auth.anyRequest().authenticated();
                        })
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(Customizer.withDefaults())
                                        .authenticationEntryPoint(
                                                (req, res, exception) ->
                                                        error(
                                                                res,
                                                                401,
                                                                "UNAUTHORIZED",
                                                                req.getRequestURI())))
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (req, res, exception) ->
                                                        error(
                                                                res,
                                                                401,
                                                                "UNAUTHORIZED",
                                                                req.getRequestURI()))
                                        .accessDeniedHandler(
                                                (req, res, exception) ->
                                                        error(
                                                                res,
                                                                403,
                                                                "FORBIDDEN",
                                                                req.getRequestURI())));
        return http.build();
    }

    private void error(HttpServletResponse response, int status, String code, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(
                response.getOutputStream(),
                new ApiError(
                        code, status == 401 ? "Authentication required" : "Access denied", path));
    }

    @Bean
    public UrlBasedCorsConfigurationSource cors(
            @Value("${wavelength.cors-origins}") String origins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(
                Arrays.stream(origins.split(",", -1)).map(TextInput::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

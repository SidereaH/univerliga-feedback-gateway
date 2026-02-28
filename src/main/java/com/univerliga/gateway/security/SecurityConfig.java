package com.univerliga.gateway.security;

import com.univerliga.gateway.error.ApiError;
import com.univerliga.gateway.error.ApiErrorResponse;
import com.univerliga.gateway.util.RequestIdHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestId = RequestIdHolder.get();
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setHeader("X-Request-Id", requestId == null ? "" : requestId);
                    objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                        new ApiError("UNAUTHORIZED", "Authentication required", List.of(), requestId)
                    ));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String requestId = RequestIdHolder.get();
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setHeader("X-Request-Id", requestId == null ? "" : requestId);
                    objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                        new ApiError("FORBIDDEN", "Access denied", List.of(), requestId)
                    ));
                }))
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRoleJwtConverter());
        return converter;
    }

    @Bean
    GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> authorities;
    }
}

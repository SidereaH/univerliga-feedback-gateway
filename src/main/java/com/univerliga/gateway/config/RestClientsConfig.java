package com.univerliga.gateway.config;

import com.univerliga.gateway.util.RequestIdHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RestClientsConfig {

    @Bean
    RestClient crmRestClient(GatewayProperties properties) {
        return build(properties.clients().crm().baseUrl());
    }

    @Bean
    RestClient feedbackRestClient(GatewayProperties properties) {
        return build(properties.clients().feedback().baseUrl());
    }

    @Bean
    RestClient reportingRestClient(GatewayProperties properties) {
        return build(properties.clients().reporting().baseUrl());
    }

    @Bean
    RestClient analyticsRestClient(GatewayProperties properties) {
        return build(properties.clients().analytics().baseUrl());
    }

    private RestClient build(String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor((request, body, execution) -> {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
                }
                String requestId = RequestIdHolder.get();
                if (requestId != null && !requestId.isBlank()) {
                    request.getHeaders().set("X-Request-Id", requestId);
                }
                if (!request.getHeaders().containsKey(HttpHeaders.ACCEPT)) {
                    request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
                }
                return execution.execute(request, body);
            })
            .build();
    }
}

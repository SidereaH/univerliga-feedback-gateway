package com.univerliga.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(String mode, String version, Clients clients, Auth auth) {

    public record Clients(ServiceEndpoint crm, ServiceEndpoint feedback, ServiceEndpoint analytics) {
    }

    public record ServiceEndpoint(String baseUrl) {
    }

    public record Auth(String tokenUrl, String clientId, String clientSecret) {
    }
}

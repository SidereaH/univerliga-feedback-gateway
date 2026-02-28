package com.univerliga.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(String mode, String version, Clients clients) {

    public record Clients(ServiceEndpoint crm, ServiceEndpoint feedback, ServiceEndpoint reporting) {
    }

    public record ServiceEndpoint(String baseUrl) {
    }
}

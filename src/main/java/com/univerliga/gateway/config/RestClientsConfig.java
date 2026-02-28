package com.univerliga.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RestClientsConfig {

    @Bean
    RestClient crmRestClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.clients().crm().baseUrl()).build();
    }

    @Bean
    RestClient feedbackRestClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.clients().feedback().baseUrl()).build();
    }

    @Bean
    RestClient reportingRestClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.clients().reporting().baseUrl()).build();
    }
}

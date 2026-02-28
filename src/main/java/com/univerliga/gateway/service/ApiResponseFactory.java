package com.univerliga.gateway.service;

import com.univerliga.gateway.config.GatewayProperties;
import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.MetaDto;
import com.univerliga.gateway.util.RequestIdHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiResponseFactory {
    private final GatewayProperties properties;

    public ApiResponseFactory(GatewayProperties properties) {
        this.properties = properties;
    }

    public <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(
            data,
            new MetaDto(RequestIdHolder.get(), Instant.now().toString(), properties.version())
        );
    }
}

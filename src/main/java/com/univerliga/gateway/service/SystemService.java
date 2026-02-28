package com.univerliga.gateway.service;

import com.univerliga.gateway.config.GatewayProperties;
import com.univerliga.gateway.dto.SystemVersionResponse;
import org.springframework.stereotype.Service;

@Service
public class SystemService {
    private final GatewayProperties gatewayProperties;

    public SystemService(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public SystemVersionResponse version() {
        return new SystemVersionResponse("univerliga-gateway", "0.1.0", gatewayProperties.mode());
    }
}

package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.SystemVersionResponse;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/system", produces = "application/json;charset=UTF-8")
@Tag(name = "System", description = "System metadata endpoints")
public class SystemController {
    private final SystemService systemService;
    private final ApiResponseFactory responseFactory;

    public SystemController(SystemService systemService, ApiResponseFactory responseFactory) {
        this.systemService = systemService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/version")
    @Operation(summary = "Gateway version and mode", description = "Returns service name, build version and runtime mode")
    public ApiEnvelope<SystemVersionResponse> version() {
        return responseFactory.ok(systemService.version());
    }
}

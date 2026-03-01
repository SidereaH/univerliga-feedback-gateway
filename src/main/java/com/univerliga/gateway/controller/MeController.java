package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.MeResponse;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json;charset=UTF-8")
@Tag(name = "Auth", description = "Authenticated user endpoints")
public class MeController {
    private final MeService meService;
    private final ApiResponseFactory responseFactory;

    public MeController(MeService meService, ApiResponseFactory responseFactory) {
        this.meService = meService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile info resolved from JWT and CRM service")
    public ApiEnvelope<MeResponse> me() {
        return responseFactory.ok(meService.me());
    }
}

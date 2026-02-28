package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.AuthDtos;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = "application/json;charset=UTF-8")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    private final AuthService authService;
    private final ApiResponseFactory responseFactory;

    public AuthController(AuthService authService, ApiResponseFactory responseFactory) {
        this.authService = authService;
        this.responseFactory = responseFactory;
    }

    @PostMapping(value = "/token", consumes = "application/json")
    @Operation(summary = "Issue access token", description = "Authenticates user credentials and returns OAuth2 tokens", security = {})
    public ApiEnvelope<AuthDtos.TokenResponse> token(@RequestBody @Valid AuthDtos.TokenRequest request) {
        return responseFactory.ok(authService.token(request));
    }
}

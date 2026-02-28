package com.univerliga.gateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    @Schema(description = "Credentials for password grant authentication")
    public record TokenRequest(
        @NotBlank
        @Schema(example = "employee")
        String username,
        @NotBlank
        @Schema(example = "employee")
        String password
    ) {
    }

    @Schema(description = "OAuth2 token response")
    public record TokenResponse(
        @JsonAlias("access_token")
        @Schema(example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,
        @JsonAlias("expires_in")
        @Schema(example = "300")
        long expiresIn,
        @JsonAlias("refresh_expires_in")
        @Schema(example = "1800")
        long refreshExpiresIn,
        @JsonAlias("refresh_token")
        @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,
        @JsonAlias("token_type")
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(example = "profile email")
        String scope
    ) {
    }
}

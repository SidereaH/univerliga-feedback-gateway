package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gateway build and runtime mode")
public record SystemVersionResponse(
    @Schema(example = "univerliga-gateway") String name,
    @Schema(example = "0.1.0") String version,
    @Schema(example = "mock") String mode
) {
}

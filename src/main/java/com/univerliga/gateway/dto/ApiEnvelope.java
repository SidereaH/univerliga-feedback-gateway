package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard successful API response")
public record ApiEnvelope<T>(
    @Schema(description = "Payload") T data,
    @Schema(description = "Metadata") MetaDto meta
) {
}

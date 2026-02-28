package com.univerliga.gateway.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Error body")
public record ApiError(
    @Schema(example = "VALIDATION_ERROR") String code,
    @Schema(example = "Validation failed") String message,
    List<ApiErrorDetail> details,
    @Schema(example = "dbe2ea9f-82a4-4f2e-a0af-eb84611ed2c2") String requestId
) {
}

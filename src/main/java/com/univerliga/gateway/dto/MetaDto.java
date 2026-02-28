package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response metadata")
public record MetaDto(
    @Schema(example = "dbe2ea9f-82a4-4f2e-a0af-eb84611ed2c2") String requestId,
    @Schema(example = "2026-02-28T10:15:30Z") String timestamp,
    @Schema(example = "v1") String version
) {
}

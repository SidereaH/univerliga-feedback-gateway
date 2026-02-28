package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pagination block")
public record PageDto(
    @Schema(example = "1") int page,
    @Schema(example = "20") int size,
    @Schema(example = "100") long totalItems,
    @Schema(example = "5") int totalPages
) {
}

package com.univerliga.gateway.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error detail")
public record ApiErrorDetail(
    @Schema(example = "email") String field,
    @Schema(example = "must be a well-formed email address") String issue
) {
}

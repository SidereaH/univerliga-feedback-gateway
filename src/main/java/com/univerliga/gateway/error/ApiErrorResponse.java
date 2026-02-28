package com.univerliga.gateway.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response")
public record ApiErrorResponse(ApiError error) {
}

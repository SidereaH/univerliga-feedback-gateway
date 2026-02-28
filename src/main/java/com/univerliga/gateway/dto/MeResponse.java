package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Current authenticated user profile")
public record MeResponse(
    @Schema(example = "p_employee") String personId,
    @Schema(example = "employee") String username,
    @ArraySchema(schema = @Schema(example = "ROLE_EMPLOYEE")) List<String> roles,
    @Schema(example = "d_10") String departmentId,
    @Schema(example = "t_5") String teamId,
    @Schema(example = "Employee User") String displayName
) {
}

package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class PersonDtos {
    private PersonDtos() {
    }

    @Schema(description = "Short person card")
    public record PersonSummary(
        @Schema(example = "p_1") String id,
        @Schema(example = "Ivan Petrov") String displayName,
        @Schema(example = "ivan@example.com") String email,
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "true") boolean active,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt
    ) {
    }

    @Schema(description = "Detailed person card")
    public record PersonDetails(
        @Schema(example = "p_1") String id,
        @Schema(example = "Ivan Petrov") String displayName,
        @Schema(example = "ivan@example.com") String email,
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "true") boolean active,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt,
        @Schema(example = "PROVISIONED") String identityStatus,
        @Schema(example = "kc_123") String keycloakUserId
    ) {
    }

    @Schema(description = "Paginated people list")
    public record PeoplePage(
        @ArraySchema(schema = @Schema(implementation = PersonSummary.class)) List<PersonSummary> items,
        @Schema(description = "Pagination data") PageDto page
    ) {
    }

    @Schema(description = "Create person request")
    public record CreatePersonRequest(
        @NotBlank @Size(max = 200) @Schema(example = "Ivan Petrov") String displayName,
        @NotBlank @Email @Schema(example = "ivan@example.com") String email,
        @NotBlank @Schema(example = "d_1") String departmentId,
        @NotBlank @Schema(example = "t_1") String teamId,
        @NotBlank @Pattern(regexp = "EMPLOYEE|MANAGER|ADMIN") @Schema(example = "EMPLOYEE", allowableValues = {"EMPLOYEE", "MANAGER", "ADMIN"}) String role
    ) {
    }

    @Schema(description = "Patch person request")
    public record PatchPersonRequest(
        @Size(max = 200) @Schema(example = "Ivan P.") String displayName,
        @Email @Schema(example = "new.mail@example.com") String email,
        @Schema(example = "d_2") String departmentId,
        @Schema(example = "t_3") String teamId,
        @Schema(example = "false") Boolean active
    ) {
    }

    @Schema(description = "Delete operation result")
    public record DeleteResult(@Schema(example = "true") boolean deleted) {
    }
}

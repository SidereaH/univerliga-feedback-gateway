package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class TaskDtos {
    private TaskDtos() {
    }

    @Schema(description = "Task period")
    public record PeriodDto(
        @NotNull @Schema(example = "2026-01-01") String from,
        @NotNull @Schema(example = "2026-01-31") String to
    ) {
    }

    @Schema(description = "Task card")
    public record TaskResponse(
        @Schema(example = "task_1") String id,
        @Schema(example = "Quarter review") String title,
        @Schema(example = "Review Q1 outcomes") String description,
        @Schema(example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "CLOSED"}) String status,
        PeriodDto period,
        @Schema(example = "p_10") String ownerId,
        @Schema(example = "p_11") String assigneeId,
        @ArraySchema(schema = @Schema(example = "p_11")) List<String> participantIds,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt
    ) {
    }

    @Schema(description = "Paginated tasks list")
    public record TaskPage(
        @ArraySchema(schema = @Schema(implementation = TaskResponse.class)) List<TaskResponse> items,
        PageDto page
    ) {
    }

    @Schema(description = "Create task request")
    public record CreateTaskRequest(
        @NotBlank @Size(max = 200) @Schema(example = "Quarter review") String title,
        @Size(max = 2000) @Schema(example = "Review Q1 outcomes") String description,
        @NotNull @Valid PeriodDto period,
        @NotBlank @Schema(example = "p_10") String ownerId,
        @NotBlank @Schema(example = "p_11") String assigneeId,
        @NotEmpty @ArraySchema(schema = @Schema(example = "p_11")) List<String> participantIds
    ) {
    }

    @Schema(description = "Patch task request")
    public record PatchTaskRequest(
        @Size(max = 200) @Schema(example = "Quarter review updated") String title,
        @Size(max = 2000) @Schema(example = "Updated description") String description,
        @Schema(example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "CLOSED"}) String status,
        @Valid PeriodDto period,
        @Schema(example = "p_12") String assigneeId,
        @ArraySchema(schema = @Schema(example = "p_11")) List<String> participantIds
    ) {
    }

    @Schema(description = "Close task response")
    public record CloseTaskResponse(
        @Schema(example = "CLOSED") String status,
        @Schema(example = "2026-02-28T09:00:00Z") String closedAt
    ) {
    }
}

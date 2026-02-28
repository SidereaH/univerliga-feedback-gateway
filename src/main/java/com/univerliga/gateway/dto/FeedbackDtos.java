package com.univerliga.gateway.dto;

import com.univerliga.gateway.model.FeedbackRecord;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class FeedbackDtos {
    private FeedbackDtos() {
    }

    @Schema(description = "Feedback subcategory/tag")
    public record SubcategoryDto(
        @Schema(example = "sub_comm_good") String id,
        @Schema(example = "Доброжелательная / корректная коммуникация") String name,
        @Schema(example = "POSITIVE") String polarity,
        @Schema(example = "true") boolean active
    ) {
    }

    @Schema(description = "Feedback category")
    public record CategoryDto(
        @Schema(example = "cat_work") String id,
        @Schema(example = "По работе") String name,
        @ArraySchema(schema = @Schema(implementation = SubcategoryDto.class)) List<SubcategoryDto> subcategories
    ) {
    }

    @Schema(description = "Categories response")
    public record CategoriesResponse(
        @ArraySchema(schema = @Schema(implementation = CategoryDto.class)) List<CategoryDto> items
    ) {
    }

    @Schema(description = "Create review request")
    public record CreateReviewRequest(
        @NotBlank @Schema(example = "p_11") String targetPersonId,
        @Schema(example = "TASK", nullable = true) FeedbackRecord.ContextType contextType,
        @Schema(example = "task_1", nullable = true) String contextRef,
        @Schema(example = "task_1", nullable = true, deprecated = true,
            description = "Backward compatibility. If provided and contextType/contextRef absent then mapped to TASK/taskId")
        String taskId,
        @Min(1) @Max(5) @Schema(example = "5", nullable = true, minimum = "1", maximum = "5") Integer rating,
        @Schema(example = "POSITIVE", nullable = true) FeedbackRecord.Sentiment sentiment,
        @Size(min = 1, max = 3) @ArraySchema(schema = @Schema(example = "sub_comm_good")) List<String> tagIds,
        @Size(max = 2000) @Schema(example = "Great collaboration") String comment
    ) {
    }

    @Schema(description = "Update review request. target/context/author are immutable.")
    public record UpdateReviewRequest(
        @Min(1) @Max(5) @Schema(example = "4", nullable = true, minimum = "1", maximum = "5") Integer rating,
        @Schema(example = "POSITIVE", nullable = true) FeedbackRecord.Sentiment sentiment,
        @Size(min = 1, max = 3) @ArraySchema(schema = @Schema(example = "sub_help_explain")) List<String> tagIds,
        @Size(max = 2000) @Schema(example = "Updated comment") String comment
    ) {
    }

    @Schema(description = "Review visibility settings")
    public record VisibilityDto(
        @Schema(example = "true") boolean authorHidden
    ) {
    }

    @Schema(description = "Review response (author hidden)")
    public record ReviewResponse(
        @Schema(example = "fb_1") String id,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt,
        @Schema(example = "2026-01-01T11:00:00Z", nullable = true) String updatedAt,
        @Schema(example = "p_11") String targetPersonId,
        @Schema(example = "TASK") FeedbackRecord.ContextType contextType,
        @Schema(example = "task_1") String contextRef,
        @Schema(example = "5", nullable = true) Integer rating,
        @Schema(example = "POSITIVE", nullable = true) FeedbackRecord.Sentiment sentiment,
        @ArraySchema(schema = @Schema(example = "sub_comm_good")) List<String> tagIds,
        @Schema(example = "Great collaboration") String comment,
        VisibilityDto visibility
    ) {
    }

    @Schema(description = "Review response for raw feed (author visible)")
    public record RawReviewResponse(
        @Schema(example = "fb_1") String id,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt,
        @Schema(example = "2026-01-01T11:00:00Z", nullable = true) String updatedAt,
        @Schema(example = "p_11") String targetPersonId,
        @Schema(example = "p_12") String authorPersonId,
        @Schema(example = "TASK") FeedbackRecord.ContextType contextType,
        @Schema(example = "task_1") String contextRef,
        @Schema(example = "4", nullable = true) Integer rating,
        @Schema(example = "NEGATIVE", nullable = true) FeedbackRecord.Sentiment sentiment,
        @ArraySchema(schema = @Schema(example = "sub_deadline_fail")) List<String> tagIds,
        @Schema(example = "Needs better planning") String comment,
        VisibilityDto visibility
    ) {
    }

    @Schema(description = "Paginated review list")
    public record ReviewPage(
        @ArraySchema(schema = @Schema(description = "Review items")) List<?> items,
        @Schema(description = "Pagination data") PageDto page
    ) {
    }
}

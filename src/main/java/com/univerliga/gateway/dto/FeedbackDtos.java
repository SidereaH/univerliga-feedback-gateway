package com.univerliga.gateway.dto;

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

    @Schema(description = "Feedback subcategory")
    public record SubcategoryDto(
        @Schema(example = "sub_1") String id,
        @Schema(example = "Communication") String name
    ) {
    }

    @Schema(description = "Feedback category")
    public record CategoryDto(
        @Schema(example = "cat_1") String id,
        @Schema(example = "Performance") String name,
        @ArraySchema(schema = @Schema(implementation = SubcategoryDto.class)) List<SubcategoryDto> subcategories
    ) {
    }

    @Schema(description = "Categories response")
    public record CategoriesResponse(
        @ArraySchema(schema = @Schema(implementation = CategoryDto.class)) List<CategoryDto> items
    ) {
    }

    @Schema(description = "Create feedback request")
    public record CreateFeedbackRequest(
        @NotBlank @Schema(example = "task_1") String taskId,
        @NotBlank @Schema(example = "p_11") String targetPersonId,
        @NotBlank @Schema(example = "cat_1") String categoryId,
        @NotBlank @Schema(example = "sub_1") String subcategoryId,
        @Min(1) @Max(5) @Schema(example = "5", minimum = "1", maximum = "5") int rating,
        @Size(max = 2000) @Schema(example = "Great collaboration") String comment
    ) {
    }

    @Schema(description = "Feedback visibility settings")
    public record VisibilityDto(@Schema(example = "true") boolean authorHidden) {
    }

    @Schema(description = "Feedback item for author/raw views")
    public record FeedbackItem(
        @Schema(example = "fb_1") String id,
        @Schema(example = "task_1") String taskId,
        @Schema(example = "p_11") String targetPersonId,
        @Schema(example = "p_12") String authorPersonId,
        @Schema(example = "cat_1") String categoryId,
        @Schema(example = "sub_1") String subcategoryId,
        @Schema(example = "5") int rating,
        @Schema(example = "Great collaboration") String comment,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt,
        @Schema(description = "Visibility flags") VisibilityDto visibility
    ) {
    }

    @Schema(description = "Feedback item for inbox view")
    public record FeedbackInboxItem(
        @Schema(example = "fb_1") String id,
        @Schema(example = "task_1") String taskId,
        @Schema(example = "p_11") String targetPersonId,
        @Schema(example = "cat_1") String categoryId,
        @Schema(example = "sub_1") String subcategoryId,
        @Schema(example = "4") int rating,
        @Schema(example = "Thanks") String comment,
        @Schema(example = "2026-01-01T10:00:00Z") String createdAt,
        @Schema(description = "Visibility flags") VisibilityDto visibility
    ) {
    }

    @Schema(description = "Paginated feedback list")
    public record FeedbackPage(
        @ArraySchema(schema = @Schema(description = "Feedback items")) List<?> items,
        @Schema(description = "Pagination data") PageDto page
    ) {
    }
}

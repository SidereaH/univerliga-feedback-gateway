package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.FeedbackDtos;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/feedback", produces = "application/json;charset=UTF-8")
@Tag(name = "Feedback", description = "Context-based feedback endpoints")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final ApiResponseFactory responseFactory;

    public FeedbackController(FeedbackService feedbackService, ApiResponseFactory responseFactory) {
        this.feedbackService = feedbackService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/categories")
    @Operation(summary = "Feedback categories", description = "Returns categories with polarity and active flags for tags")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.CategoriesResponse> categories() {
        return responseFactory.ok(feedbackService.categories());
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create review", description = "Creates review with contextType/contextRef; supports legacy taskId mapping")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.ReviewResponse> create(@RequestBody @Valid FeedbackDtos.CreateReviewRequest request) {
        return responseFactory.ok(feedbackService.create(request));
    }

    @PutMapping(value = "/{reviewId}", consumes = "application/json")
    @Operation(summary = "Update review", description = "Updates mutable review fields: rating/sentiment/tagIds/comment")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.ReviewResponse> update(@PathVariable String reviewId,
                                                           @RequestBody @Valid FeedbackDtos.UpdateReviewRequest request) {
        return responseFactory.ok(feedbackService.update(reviewId, request));
    }

    @GetMapping("/my")
    @Operation(summary = "My sent feedback", description = "Returns reviews authored by current user")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.ReviewPage> my(@Parameter(description = "Context type filter") @RequestParam(required = false) FeedbackRecord.ContextType contextType,
                                                   @Parameter(description = "Context reference filter") @RequestParam(required = false) String contextRef,
                                                   @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                   @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.my(contextType, contextRef, page, size));
    }

    @GetMapping("/inbox")
    @Operation(summary = "Inbox reviews", description = "Returns target/team inbox reviews with hidden author")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.ReviewPage> inbox(@Parameter(description = "Context type filter") @RequestParam(required = false) FeedbackRecord.ContextType contextType,
                                                      @Parameter(description = "Context reference filter") @RequestParam(required = false) String contextRef,
                                                      @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.inbox(contextType, contextRef, page, size));
    }

    @GetMapping("/raw")
    @Operation(summary = "Raw reviews with author", description = "Administrative endpoint for ADMIN/HR with visible authorPersonId")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiEnvelope<FeedbackDtos.ReviewPage> raw(@Parameter(description = "Context type filter") @RequestParam(required = false) FeedbackRecord.ContextType contextType,
                                                    @Parameter(description = "Context reference filter") @RequestParam(required = false) String contextRef,
                                                    @Parameter(description = "Target person filter") @RequestParam(required = false) String targetPersonId,
                                                    @Parameter(description = "Author person filter") @RequestParam(required = false) String authorPersonId,
                                                    @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                    @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.raw(contextType, contextRef, targetPersonId, authorPersonId, page, size));
    }
}

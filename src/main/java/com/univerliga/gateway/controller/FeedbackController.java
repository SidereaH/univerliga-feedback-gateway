package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.FeedbackDtos;
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
@Tag(name = "Feedback", description = "Feedback and survey endpoints")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final ApiResponseFactory responseFactory;

    public FeedbackController(FeedbackService feedbackService, ApiResponseFactory responseFactory) {
        this.feedbackService = feedbackService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/categories")
    @Operation(summary = "Feedback categories", description = "Returns available feedback categories and subcategories")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.CategoriesResponse> categories() {
        return responseFactory.ok(feedbackService.categories());
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create feedback", description = "Creates feedback for selected task and target person")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.FeedbackItem> create(@RequestBody @Valid FeedbackDtos.CreateFeedbackRequest request) {
        return responseFactory.ok(feedbackService.create(request));
    }

    @GetMapping("/my")
    @Operation(summary = "My sent feedback", description = "Returns feedback authored by current user")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.FeedbackPage> my(@Parameter(description = "Filter by task id") @RequestParam(required = false) String taskId,
                                                     @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                     @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.my(taskId, page, size));
    }

    @GetMapping("/inbox")
    @Operation(summary = "My incoming feedback", description = "Returns feedback where current user is target")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<FeedbackDtos.FeedbackPage> inbox(@Parameter(description = "Filter by task id") @RequestParam(required = false) String taskId,
                                                        @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.inbox(taskId, page, size));
    }

    @GetMapping("/raw")
    @Operation(summary = "Raw feedback with author", description = "Administrative endpoint returning raw feedback with author person id")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiEnvelope<FeedbackDtos.FeedbackPage> raw(@Parameter(description = "Filter by task id") @RequestParam(required = false) String taskId,
                                                      @Parameter(description = "Filter by target person id") @RequestParam(required = false) String targetPersonId,
                                                      @Parameter(description = "Filter by author person id") @RequestParam(required = false) String authorPersonId,
                                                      @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(feedbackService.raw(taskId, targetPersonId, authorPersonId, page, size));
    }
}

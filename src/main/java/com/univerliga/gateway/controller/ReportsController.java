package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/reports", produces = "application/json;charset=UTF-8")
@Tag(name = "Reporting", description = "Reporting and analytics endpoints")
public class ReportsController {
    private final ReportingService reportingService;
    private final ApiResponseFactory responseFactory;

    public ReportsController(ReportingService reportingService, ApiResponseFactory responseFactory) {
        this.reportingService = reportingService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/summary")
    @Operation(summary = "Summary report with coverage", description = "Returns top-level KPIs including coverage for selected scope")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.SummaryResponse> summary(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                           @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                           @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                           @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                           @Parameter(description = "Person filter") @RequestParam(required = false) String personId) {
        return responseFactory.ok(reportingService.summary(periodFrom, periodTo, departmentId, teamId, personId));
    }

    @GetMapping("/charts/ratings-by-category")
    @Operation(summary = "Ratings grouped by category", description = "Builds category bar chart for selected period and scope")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.RatingsByCategoryResponse> ratingsByCategory(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                @Parameter(description = "Person filter") @RequestParam(required = false) String personId) {
        return responseFactory.ok(reportingService.ratingsByCategory(periodFrom, periodTo, departmentId, teamId, personId));
    }

    @GetMapping("/charts/trend")
    @Operation(summary = "Trend report", description = "Builds month/week trend for responses, avgRating, positiveShare, negativeShare")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.TrendResponse> trend(@Parameter(description = "Metric: responses|avgRating|positiveShare|negativeShare") @RequestParam String metric,
                                                       @Parameter(description = "Granularity: month|week") @RequestParam(defaultValue = "month") String granularity,
                                                       @Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String from,
                                                       @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String to,
                                                       @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                       @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                       @Parameter(description = "Person filter") @RequestParam(required = false) String personId) {
        return responseFactory.ok(reportingService.trend(metric, granularity, from, to, departmentId, teamId, personId));
    }

    @GetMapping("/charts/positivity-by-person")
    @Operation(summary = "Positivity/negativity balance by person", description = "Returns ranking with positive/negative counts and average rating")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.PositivityByPersonResponse> positivityByPerson(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                  @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                  @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                  @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                  @Parameter(description = "Maximum items in result") @RequestParam(defaultValue = "20") int limit,
                                                                                  @Parameter(description = "Sort key: total|positive|negative|avgRating") @RequestParam(defaultValue = "total") String sort) {
        return responseFactory.ok(reportingService.positivityByPerson(periodFrom, periodTo, departmentId, teamId, limit, sort));
    }

    @GetMapping("/charts/subcategory-frequency")
    @Operation(summary = "Subcategory frequency", description = "Returns tag frequency split by polarity")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.SubcategoryFrequencyResponse> subcategoryFrequency(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                      @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                      @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                      @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                      @Parameter(description = "Person filter") @RequestParam(required = false) String personId,
                                                                                      @Parameter(description = "Category filter") @RequestParam(required = false) String categoryId,
                                                                                      @Parameter(description = "Maximum items in result") @RequestParam(defaultValue = "30") int limit,
                                                                                      @Parameter(description = "Sort key: total|positive|negative") @RequestParam(defaultValue = "total") String sort) {
        return responseFactory.ok(reportingService.subcategoryFrequency(periodFrom, periodTo, departmentId, teamId, personId, categoryId, limit, sort));
    }

    @GetMapping("/insights/top-tags")
    @Operation(summary = "Top tags split by polarity", description = "Returns top positive and top negative tags for selected scope")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.TopTagsResponse> topTags(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                           @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                           @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                           @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                           @Parameter(description = "Person filter") @RequestParam(required = false) String personId,
                                                           @Parameter(description = "Maximum items per polarity") @RequestParam(defaultValue = "5") int limit) {
        return responseFactory.ok(reportingService.topTags(periodFrom, periodTo, departmentId, teamId, personId, limit));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Composite dashboard", description = "Returns KPIs with coverage, chart blocks and top-tags insights")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiEnvelope<ReportDtos.DashboardResponse> dashboard(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                               @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                               @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                               @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                               @Parameter(description = "Person filter") @RequestParam(required = false) String personId) {
        return responseFactory.ok(reportingService.dashboard(periodFrom, periodTo, departmentId, teamId, personId));
    }
}

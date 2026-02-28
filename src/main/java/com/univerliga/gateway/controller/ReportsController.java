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
    @Operation(summary = "Summary report", description = "Returns top-level KPIs for selected period and scope")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.SummaryResponse> summary(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                           @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                           @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                           @Parameter(description = "Team filter") @RequestParam(required = false) String teamId) {
        return responseFactory.ok(reportingService.summary(periodFrom, periodTo, departmentId, teamId));
    }

    @GetMapping("/charts/ratings-by-category")
    @Operation(summary = "Ratings grouped by category", description = "Builds category bar chart for selected period")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.RatingsByCategoryResponse> ratingsByCategory(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                @Parameter(description = "Team filter") @RequestParam(required = false) String teamId) {
        return responseFactory.ok(reportingService.ratingsByCategory(periodFrom, periodTo, teamId));
    }

    @GetMapping("/charts/trend")
    @Operation(summary = "Trend report", description = "Builds time series trend chart")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.TrendResponse> trend(@Parameter(description = "Metric: responses or avgRating") @RequestParam String metric,
                                                       @Parameter(description = "Trend granularity") @RequestParam(defaultValue = "month") String period,
                                                       @Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String from,
                                                       @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String to,
                                                       @Parameter(description = "Team filter") @RequestParam(required = false) String teamId) {
        return responseFactory.ok(reportingService.trend(metric, period, from, to, teamId));
    }

    @GetMapping("/charts/positivity-by-person")
    @Operation(summary = "Positivity/negativity balance by person", description = "Returns ranking with positive/negative counts and average rating")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.PositivityByPersonResponse> positivityByPerson(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                  @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                  @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                  @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                  @Parameter(description = "Maximum items in result") @RequestParam(defaultValue = "20") int limit,
                                                                                  @Parameter(description = "Sort key: total|positive|negative|avgRating") @RequestParam(defaultValue = "total") String sort) {
        return responseFactory.ok(reportingService.positivityByPerson(periodFrom, periodTo, departmentId, teamId, limit, sort));
    }

    @GetMapping("/charts/subcategory-frequency")
    @Operation(summary = "Subcategory frequency", description = "Returns top subcategories by positive/negative frequency")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.SubcategoryFrequencyResponse> subcategoryFrequency(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                      @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                      @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                      @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                      @Parameter(description = "Category filter") @RequestParam(required = false) String categoryId,
                                                                                      @Parameter(description = "Maximum items in result") @RequestParam(defaultValue = "30") int limit,
                                                                                      @Parameter(description = "Sort key: total|positive|negative") @RequestParam(defaultValue = "total") String sort) {
        return responseFactory.ok(reportingService.subcategoryFrequency(periodFrom, periodTo, departmentId, teamId, categoryId, limit, sort));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Composite manager dashboard", description = "Returns KPI block and all dashboard charts in one request")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.DashboardResponse> dashboard(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                               @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                               @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                               @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                               @Parameter(description = "Optional person filter") @RequestParam(required = false) String personId) {
        return responseFactory.ok(reportingService.dashboard(periodFrom, periodTo, departmentId, teamId, personId));
    }

    @GetMapping("/insights/top-subcategories")
    @Operation(summary = "Top best/worst subcategories", description = "Returns top subcategories by average rating")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<ReportDtos.TopSubcategoriesInsightsResponse> topSubcategories(@Parameter(description = "Period start (YYYY-MM-DD)") @RequestParam String periodFrom,
                                                                                      @Parameter(description = "Period end (YYYY-MM-DD)") @RequestParam String periodTo,
                                                                                      @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                                                      @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                                                      @Parameter(description = "Maximum number of best/worst items") @RequestParam(defaultValue = "5") int limit) {
        return responseFactory.ok(reportingService.topSubcategories(periodFrom, periodTo, departmentId, teamId, limit));
    }
}

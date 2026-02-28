package com.univerliga.gateway.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class ReportDtos {
    private ReportDtos() {
    }

    @Schema(description = "Requested reporting period")
    public record ReportPeriod(
        @Schema(example = "2026-01-01") String from,
        @Schema(example = "2026-01-31") String to
    ) {
    }

    @Schema(description = "Scope filters")
    public record ScopeDto(
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId
    ) {
    }

    @Schema(description = "Scope filters with person")
    public record ScopeWithPersonDto(
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "p_11") String personId
    ) {
    }

    @Schema(description = "Scope filters with category")
    public record ScopeWithCategoryDto(
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "p_11") String personId,
        @Schema(example = "cat_work") String categoryId
    ) {
    }

    @Schema(description = "Feedback coverage KPI")
    public record CoverageKpi(
        @Schema(example = "7") long targetsWithAtLeastOne,
        @Schema(example = "10") long totalTargetsInScope,
        @Schema(example = "0.7") double share
    ) {
    }

    @Schema(description = "Summary KPI block")
    public record SummaryKpis(
        @Schema(example = "120") long responses,
        @Schema(example = "85") long positive,
        @Schema(example = "35") long negative,
        @Schema(example = "34") long uniqueAuthors,
        @Schema(example = "18") long uniqueTargets,
        @Schema(example = "4.2") double avgRating,
        @Schema(example = "0.71") double positiveShare,
        @Schema(example = "0.29") double negativeShare,
        CoverageKpi coverage
    ) {
    }

    @Schema(description = "Summary response")
    public record SummaryResponse(
        ReportPeriod period,
        ScopeWithPersonDto scope,
        SummaryKpis kpis
    ) {
    }

    @Schema(description = "Category rating series item")
    public record CategorySeriesItem(
        @Schema(example = "cat_work") String categoryId,
        @Schema(example = "По работе") String categoryName,
        @Schema(example = "4.1") double avgRating,
        @Schema(example = "55") long count
    ) {
    }

    @Schema(description = "Ratings by category response")
    public record RatingsByCategoryResponse(
        @ArraySchema(schema = @Schema(implementation = CategorySeriesItem.class)) List<CategorySeriesItem> series
    ) {
    }

    @Schema(description = "Trend point")
    public record TrendPoint(
        @Schema(example = "2026-01") String x,
        @Schema(example = "12") double y
    ) {
    }

    @Schema(description = "Trend response")
    public record TrendResponse(
        @Schema(example = "responses") String metric,
        @Schema(example = "month") String granularity,
        @ArraySchema(schema = @Schema(implementation = TrendPoint.class)) List<TrendPoint> points
    ) {
    }

    @Schema(description = "Person positivity/negativity aggregate")
    public record PositivityByPersonItem(
        @Schema(example = "p_11") String personId,
        @Schema(example = "Ivan P.") String displayName,
        @Schema(example = "18") long positive,
        @Schema(example = "4") long negative,
        @Schema(example = "22") long total,
        @Schema(example = "4.6") double avgRating
    ) {
    }

    @Schema(description = "Positivity by person response")
    public record PositivityByPersonResponse(
        ReportPeriod period,
        ScopeWithPersonDto scope,
        @ArraySchema(schema = @Schema(implementation = PositivityByPersonItem.class)) List<PositivityByPersonItem> items
    ) {
    }

    @Schema(description = "Subcategory frequency aggregate")
    public record SubcategoryFrequencyItem(
        @Schema(example = "sub_comm_good") String subcategoryId,
        @Schema(example = "Доброжелательная / корректная коммуникация") String subcategoryName,
        @Schema(example = "25") long positive,
        @Schema(example = "2") long negative,
        @Schema(example = "27") long total
    ) {
    }

    @Schema(description = "Subcategory frequency response")
    public record SubcategoryFrequencyResponse(
        ReportPeriod period,
        ScopeWithCategoryDto scope,
        @ArraySchema(schema = @Schema(implementation = SubcategoryFrequencyItem.class)) List<SubcategoryFrequencyItem> items
    ) {
    }

    @Schema(description = "Top tag aggregate")
    public record TopTagItem(
        @Schema(example = "sub_comm_good") String subcategoryId,
        @Schema(example = "Доброжелательная / корректная коммуникация") String name,
        @Schema(example = "33") long count,
        @Schema(example = "0.31") double share
    ) {
    }

    @Schema(description = "Top tags split by polarity")
    public record TopTagsResponse(
        ReportPeriod period,
        @ArraySchema(schema = @Schema(implementation = TopTagItem.class)) List<TopTagItem> topPositive,
        @ArraySchema(schema = @Schema(implementation = TopTagItem.class)) List<TopTagItem> topNegative
    ) {
    }

    @Schema(description = "Dashboard trend block")
    public record DashboardTrend(
        @Schema(example = "responses") String metric,
        @Schema(example = "month") String granularity,
        @ArraySchema(schema = @Schema(implementation = TrendPoint.class)) List<TrendPoint> points
    ) {
    }

    @Schema(description = "Dashboard charts block")
    public record DashboardCharts(
        @ArraySchema(schema = @Schema(implementation = CategorySeriesItem.class)) List<CategorySeriesItem> ratingsByCategory,
        DashboardTrend trend,
        @ArraySchema(schema = @Schema(implementation = PositivityByPersonItem.class)) List<PositivityByPersonItem> positivityByPerson,
        @ArraySchema(schema = @Schema(implementation = SubcategoryFrequencyItem.class)) List<SubcategoryFrequencyItem> subcategoryFrequency
    ) {
    }

    @Schema(description = "Dashboard insights block")
    public record DashboardInsights(
        TopTagsResponse topTags
    ) {
    }

    @Schema(description = "Composite dashboard response")
    public record DashboardResponse(
        ReportPeriod period,
        ScopeWithPersonDto scope,
        SummaryKpis kpis,
        DashboardCharts charts,
        DashboardInsights insights
    ) {
    }
}

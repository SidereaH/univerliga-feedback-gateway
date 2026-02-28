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

    @Schema(description = "Scope filters with category")
    public record ScopeWithCategoryDto(
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "cat_1") String categoryId
    ) {
    }

    @Schema(description = "Scope filters with person")
    public record ScopeWithPersonDto(
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        @Schema(example = "p_11") String personId
    ) {
    }

    @Schema(description = "Summary KPI block")
    public record Kpis(
        @Schema(example = "120") long responses,
        @Schema(example = "4.2") double avgRating,
        @Schema(example = "0.76") double positiveShare
    ) {
    }

    @Schema(description = "Summary response")
    public record SummaryResponse(
        @Schema(description = "Requested period") ReportPeriod period,
        @Schema(description = "Computed KPI values") Kpis kpis
    ) {
    }

    @Schema(description = "Category rating series item")
    public record CategorySeriesItem(
        @Schema(example = "cat_1") String categoryId,
        @Schema(example = "Performance") String categoryName,
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
        @Schema(description = "Requested period") ReportPeriod period,
        @Schema(description = "Applied filters") ScopeDto scope,
        @ArraySchema(schema = @Schema(implementation = PositivityByPersonItem.class)) List<PositivityByPersonItem> items
    ) {
    }

    @Schema(description = "Subcategory frequency aggregate")
    public record SubcategoryFrequencyItem(
        @Schema(example = "sub_1") String subcategoryId,
        @Schema(example = "Communication") String subcategoryName,
        @Schema(example = "25") long positive,
        @Schema(example = "2") long negative,
        @Schema(example = "27") long total
    ) {
    }

    @Schema(description = "Subcategory frequency response")
    public record SubcategoryFrequencyResponse(
        @Schema(description = "Requested period") ReportPeriod period,
        @Schema(description = "Applied filters") ScopeWithCategoryDto scope,
        @ArraySchema(schema = @Schema(implementation = SubcategoryFrequencyItem.class)) List<SubcategoryFrequencyItem> items
    ) {
    }

    @Schema(description = "Dashboard KPI block")
    public record DashboardKpis(
        @Schema(example = "120") long responses,
        @Schema(example = "34") long uniqueAuthors,
        @Schema(example = "18") long uniqueTargets,
        @Schema(example = "4.2") double avgRating,
        @Schema(example = "0.76") double positiveShare,
        @Schema(example = "0.24") double negativeShare
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
        @Schema(description = "Time-series chart") DashboardTrend trend,
        @ArraySchema(schema = @Schema(implementation = PositivityByPersonItem.class)) List<PositivityByPersonItem> positivityByPerson,
        @ArraySchema(schema = @Schema(implementation = SubcategoryFrequencyItem.class)) List<SubcategoryFrequencyItem> subcategoryFrequency
    ) {
    }

    @Schema(description = "Composite dashboard response")
    public record DashboardResponse(
        @Schema(description = "Requested period") ReportPeriod period,
        @Schema(description = "Applied filters") ScopeWithPersonDto scope,
        @Schema(description = "Dashboard KPIs") DashboardKpis kpis,
        @Schema(description = "Dashboard chart groups") DashboardCharts charts
    ) {
    }

    @Schema(description = "Top-subcategory insight item")
    public record TopSubcategoryInsightItem(
        @Schema(example = "sub_1") String subcategoryId,
        @Schema(example = "Communication") String name,
        @Schema(example = "4.8") double avgRating,
        @Schema(example = "12") long count
    ) {
    }

    @Schema(description = "Best and worst subcategories response")
    public record TopSubcategoriesInsightsResponse(
        @Schema(description = "Requested period") ReportPeriod period,
        @ArraySchema(schema = @Schema(implementation = TopSubcategoryInsightItem.class)) List<TopSubcategoryInsightItem> best,
        @ArraySchema(schema = @Schema(implementation = TopSubcategoryInsightItem.class)) List<TopSubcategoryInsightItem> worst
    ) {
    }
}

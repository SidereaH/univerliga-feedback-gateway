package com.univerliga.gateway.client.real;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealReportingClient implements ReportingClient {
    private static final ParameterizedTypeReference<Envelope<SummaryResponseDto>> SUMMARY_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<RatingsByCategoryResponseDto>> RATINGS_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<TrendResponseDto>> TREND_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<PositivityByPersonResponseDto>> POSITIVITY_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<SubcategoryFrequencyResponseDto>> SUBCATEGORY_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<InsightsResponseDto>> INSIGHTS_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<DashboardResponseDto>> DASHBOARD_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final RestClient analyticsRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealReportingClient(@Qualifier("analyticsRestClient") RestClient analyticsRestClient) {
        this.analyticsRestClient = analyticsRestClient;
    }

    @Override
    public ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        SummaryResponseDto dto = callSummary(from, to, departmentId, teamId, personId);
        long positive = Math.round(dto.kpis().responses() * dto.kpis().positiveShare());
        long negative = Math.round(dto.kpis().responses() * dto.kpis().negativeShare());

        return new ReportDtos.SummaryResponse(
            new ReportDtos.ReportPeriod(dto.period().from(), dto.period().to()),
            new ReportDtos.ScopeWithPersonDto(dto.scope().departmentId(), dto.scope().teamId(), dto.scope().personId()),
            new ReportDtos.SummaryKpis(
                dto.kpis().responses(),
                positive,
                negative,
                dto.kpis().uniqueAuthors(),
                dto.kpis().uniqueTargets(),
                dto.kpis().avgRating(),
                dto.kpis().positiveShare(),
                dto.kpis().negativeShare(),
                null
            )
        );
    }

    @Override
    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        RatingsByCategoryResponseDto dto = callRatingsByCategory(from, to, departmentId, teamId, personId);
        return new ReportDtos.RatingsByCategoryResponse(
            dto.series().stream()
                .map(s -> new ReportDtos.CategorySeriesItem(s.categoryId(), s.categoryName(), s.avgRating(), s.count()))
                .toList()
        );
    }

    @Override
    public ReportDtos.TrendResponse trend(String metric, String granularity, LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        TrendResponseDto dto = callTrend(metric, granularity, from, to, departmentId, teamId, personId);
        return new ReportDtos.TrendResponse(
            dto.metric(),
            dto.granularity(),
            dto.points().stream().map(p -> new ReportDtos.TrendPoint(p.x(), p.y())).toList()
        );
    }

    @Override
    public ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from, LocalDate to, String departmentId, String teamId, int limit, String sort) {
        PositivityByPersonResponseDto dto = callPositivityByPerson(from, to, departmentId, teamId, limit, sort);
        return new ReportDtos.PositivityByPersonResponse(
            new ReportDtos.ReportPeriod(dto.period().from(), dto.period().to()),
            new ReportDtos.ScopeWithPersonDto(dto.scope().departmentId(), dto.scope().teamId(), dto.scope().personId()),
            dto.items().stream()
                .map(i -> new ReportDtos.PositivityByPersonItem(i.personId(), i.displayName(), i.positive(), i.negative(), i.total(), i.avgRating()))
                .toList()
        );
    }

    @Override
    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from,
                                                                        LocalDate to,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        String personId,
                                                                        String categoryId,
                                                                        int limit,
                                                                        String sort) {
        SubcategoryFrequencyResponseDto dto = callSubcategoryFrequency(from, to, departmentId, teamId, personId, categoryId, limit, sort);
        return new ReportDtos.SubcategoryFrequencyResponse(
            new ReportDtos.ReportPeriod(dto.period().from(), dto.period().to()),
            new ReportDtos.ScopeWithCategoryDto(
                dto.scope().departmentId(),
                dto.scope().teamId(),
                dto.scope().personId(),
                dto.scope().categoryId()
            ),
            dto.items().stream()
                .map(i -> new ReportDtos.SubcategoryFrequencyItem(i.subcategoryId(), i.subcategoryName(), i.positive(), i.negative(), i.total()))
                .toList()
        );
    }

    @Override
    public ReportDtos.TopTagsResponse topTags(LocalDate from, LocalDate to, String departmentId, String teamId, int limit) {
        InsightsResponseDto dto = callInsights(from, to, departmentId, teamId, limit);
        List<ReportDtos.TopTagItem> topPositive = toTopTags(dto.best());
        List<ReportDtos.TopTagItem> topNegative = toTopTags(dto.worst());
        return new ReportDtos.TopTagsResponse(
            new ReportDtos.ReportPeriod(dto.period().from(), dto.period().to()),
            topPositive,
            topNegative
        );
    }

    @Override
    public ReportDtos.DashboardResponse dashboard(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        DashboardResponseDto dashboard = callDashboard(from, to, departmentId, teamId, personId);
        ReportDtos.SummaryResponse summary = summary(from, to, departmentId, teamId, personId);
        ReportDtos.TopTagsResponse topTags = topTags(from, to, departmentId, teamId, 5);

        return new ReportDtos.DashboardResponse(
            new ReportDtos.ReportPeriod(dashboard.period().from(), dashboard.period().to()),
            new ReportDtos.ScopeWithPersonDto(dashboard.scope().departmentId(), dashboard.scope().teamId(), dashboard.scope().personId()),
            summary.kpis(),
            new ReportDtos.DashboardCharts(
                dashboard.charts().ratingsByCategory().stream()
                    .map(i -> new ReportDtos.CategorySeriesItem(i.categoryId(), i.categoryName(), i.avgRating(), i.count()))
                    .toList(),
                new ReportDtos.DashboardTrend(
                    dashboard.charts().trend().metric(),
                    dashboard.charts().trend().granularity(),
                    dashboard.charts().trend().points().stream().map(p -> new ReportDtos.TrendPoint(p.x(), p.y())).toList()
                ),
                dashboard.charts().positivityByPerson().stream()
                    .map(i -> new ReportDtos.PositivityByPersonItem(i.personId(), i.displayName(), i.positive(), i.negative(), i.total(), i.avgRating()))
                    .toList(),
                dashboard.charts().subcategoryFrequency().stream()
                    .map(i -> new ReportDtos.SubcategoryFrequencyItem(i.subcategoryId(), i.subcategoryName(), i.positive(), i.negative(), i.total()))
                    .toList()
            ),
            new ReportDtos.DashboardInsights(topTags)
        );
    }

    private SummaryResponseDto callSummary(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/summary")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParamIfPresent("personId", Optional.ofNullable(personId))
                    .build())
                .retrieve()
                .body(SUMMARY_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private RatingsByCategoryResponseDto callRatingsByCategory(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/charts/ratings-by-category")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParamIfPresent("personId", Optional.ofNullable(personId))
                    .build())
                .retrieve()
                .body(RATINGS_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private TrendResponseDto callTrend(String metric,
                                       String granularity,
                                       LocalDate from,
                                       LocalDate to,
                                       String departmentId,
                                       String teamId,
                                       String personId) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/charts/trend")
                    .queryParam("metric", metric)
                    .queryParam("granularity", granularity)
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParamIfPresent("personId", Optional.ofNullable(personId))
                    .build())
                .retrieve()
                .body(TREND_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private PositivityByPersonResponseDto callPositivityByPerson(LocalDate from,
                                                                 LocalDate to,
                                                                 String departmentId,
                                                                 String teamId,
                                                                 int limit,
                                                                 String sort) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/charts/positivity-by-person")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParam("limit", limit)
                    .queryParam("sort", sort)
                    .build())
                .retrieve()
                .body(POSITIVITY_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private SubcategoryFrequencyResponseDto callSubcategoryFrequency(LocalDate from,
                                                                     LocalDate to,
                                                                     String departmentId,
                                                                     String teamId,
                                                                     String personId,
                                                                     String categoryId,
                                                                     int limit,
                                                                     String sort) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/charts/subcategory-frequency")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParamIfPresent("personId", Optional.ofNullable(personId))
                    .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                    .queryParam("limit", limit)
                    .queryParam("sort", sort)
                    .build())
                .retrieve()
                .body(SUBCATEGORY_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private InsightsResponseDto callInsights(LocalDate from, LocalDate to, String departmentId, String teamId, int limit) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/insights/top-subcategories")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParam("limit", limit)
                    .build())
                .retrieve()
                .body(INSIGHTS_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DashboardResponseDto callDashboard(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        try {
            return requireData(analyticsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/reports/dashboard")
                    .queryParam("periodFrom", from)
                    .queryParam("periodTo", to)
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .queryParamIfPresent("personId", Optional.ofNullable(personId))
                    .build())
                .retrieve()
                .body(DASHBOARD_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private List<ReportDtos.TopTagItem> toTopTags(List<InsightsResponseItemDto> items) {
        long total = items.stream().mapToLong(InsightsResponseItemDto::count).sum();
        return items.stream()
            .map(i -> new ReportDtos.TopTagItem(
                i.subcategoryId(),
                i.name(),
                i.count(),
                total == 0 ? 0.0 : i.count() / (double) total
            ))
            .toList();
    }

    private <T> T requireData(Envelope<T> envelope) {
        if (envelope == null || envelope.data() == null) {
            throw new ApiException("BAD_GATEWAY", "Analytics response is empty", HttpStatus.BAD_GATEWAY);
        }
        return envelope.data();
    }

    private ApiException toApiException(RestClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            JsonNode error = body.path("error");
            String code = error.path("code").asText();
            String message = error.path("message").asText();
            if (!code.isBlank() && !message.isBlank()) {
                return new ApiException(code, message, status, extractDetails(error.path("details")));
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return new ApiException("DOWNSTREAM_ERROR", "Analytics request failed", status);
    }

    private List<ApiErrorDetail> extractDetails(JsonNode detailsNode) {
        if (!detailsNode.isArray()) {
            return List.of();
        }
        List<ApiErrorDetail> details = new ArrayList<>();
        for (JsonNode node : detailsNode) {
            String field = node.path("field").asText("");
            String issue = node.path("issue").asText(node.path("message").asText(""));
            if (!field.isBlank() || !issue.isBlank()) {
                details.add(new ApiErrorDetail(field, issue));
            }
        }
        return details;
    }

    private ApiException serviceUnavailable(RestClientException ex) {
        return new ApiException("DOWNSTREAM_UNAVAILABLE", "Analytics service unavailable", HttpStatus.BAD_GATEWAY,
            List.of(new ApiErrorDetail("analytics", ex.getMessage())));
    }

    private record Envelope<T>(T data) {
    }

    private record PeriodDto(String from, String to) {
    }

    private record ScopeDto(String departmentId, String teamId, String personId, String categoryId) {
    }

    private record SummaryKpisDto(long responses,
                                  long uniqueAuthors,
                                  long uniqueTargets,
                                  double avgRating,
                                  double positiveShare,
                                  double negativeShare) {
    }

    private record SummaryResponseDto(PeriodDto period, ScopeDto scope, SummaryKpisDto kpis) {
    }

    private record RatingsItemDto(String categoryId, String categoryName, double avgRating, long count) {
    }

    private record RatingsByCategoryResponseDto(List<RatingsItemDto> series) {
    }

    private record TrendPointDto(String x, double y) {
    }

    private record TrendResponseDto(String metric, String granularity, PeriodDto period, List<TrendPointDto> points) {
    }

    private record PositivityItemDto(String personId,
                                     String displayName,
                                     long positive,
                                     long negative,
                                     long total,
                                     double avgRating) {
    }

    private record PositivityByPersonResponseDto(PeriodDto period, ScopeDto scope, List<PositivityItemDto> items) {
    }

    private record SubcategoryFrequencyItemDto(String subcategoryId,
                                               String subcategoryName,
                                               long positive,
                                               long negative,
                                               long total) {
    }

    private record SubcategoryFrequencyResponseDto(PeriodDto period, ScopeDto scope, List<SubcategoryFrequencyItemDto> items) {
    }

    private record InsightsResponseItemDto(String subcategoryId, String name, double avgRating, long count) {
    }

    private record InsightsResponseDto(PeriodDto period, List<InsightsResponseItemDto> best, List<InsightsResponseItemDto> worst) {
    }

    private record DashboardTrendDto(String metric, String granularity, List<TrendPointDto> points) {
    }

    private record DashboardChartsDto(List<RatingsItemDto> ratingsByCategory,
                                      DashboardTrendDto trend,
                                      List<PositivityItemDto> positivityByPerson,
                                      List<SubcategoryFrequencyItemDto> subcategoryFrequency) {
    }

    private record DashboardResponseDto(PeriodDto period, ScopeDto scope, SummaryKpisDto kpis, DashboardChartsDto charts) {
    }
}

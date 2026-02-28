package com.univerliga.gateway.client.real;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealReportingClient implements ReportingClient {
    private final RestClient analyticsRestClient;

    public RealReportingClient(@Qualifier("analyticsRestClient") RestClient analyticsRestClient) {
        this.analyticsRestClient = analyticsRestClient;
    }

    @Override
    public ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API via " + analyticsRestClient);
    }

    @Override
    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }

    @Override
    public ReportDtos.TrendResponse trend(String metric, String granularity, LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }

    @Override
    public ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from, LocalDate to, String departmentId, String teamId, int limit, String sort) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }

    @Override
    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from, LocalDate to, String departmentId, String teamId, String personId, String categoryId, int limit, String sort) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }

    @Override
    public ReportDtos.TopTagsResponse topTags(LocalDate from, LocalDate to, String departmentId, String teamId, String personId, int limit) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }

    @Override
    public ReportDtos.DashboardResponse dashboard(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        throw new UnsupportedOperationException("TODO: integrate real Analytics HTTP API");
    }
}

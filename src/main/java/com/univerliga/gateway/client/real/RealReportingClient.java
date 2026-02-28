package com.univerliga.gateway.client.real;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealReportingClient implements ReportingClient {
    @Override
    public ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from, LocalDate to, String teamId) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.TrendResponse trend(String metric, String period, LocalDate from, LocalDate to, String teamId) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from, LocalDate to, String departmentId, String teamId, int limit, String sort) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from, LocalDate to, String departmentId, String teamId, String categoryId, int limit, String sort) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.DashboardResponse dashboard(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }

    @Override
    public ReportDtos.TopSubcategoriesInsightsResponse topSubcategories(LocalDate from, LocalDate to, String departmentId, String teamId, int limit) {
        throw new UnsupportedOperationException("TODO: integrate real Reporting HTTP API");
    }
}

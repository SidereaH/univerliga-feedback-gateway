package com.univerliga.gateway.service;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ReportingService {
    private final ReportingClient reportingClient;
    private final CurrentUserService currentUserService;

    public ReportingService(ReportingClient reportingClient, CurrentUserService currentUserService) {
        this.reportingClient = reportingClient;
        this.currentUserService = currentUserService;
    }

    public ReportDtos.SummaryResponse summary(String periodFrom, String periodTo, String departmentId, String teamId) {
        enforceManagerOrAdmin();
        return reportingClient.summary(parseDate(periodFrom, "periodFrom"), parseDate(periodTo, "periodTo"), departmentId, teamId);
    }

    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(String periodFrom, String periodTo, String teamId) {
        enforceManagerOrAdmin();
        return reportingClient.ratingsByCategory(parseDate(periodFrom, "periodFrom"), parseDate(periodTo, "periodTo"), teamId);
    }

    public ReportDtos.TrendResponse trend(String metric, String period, String from, String to, String teamId) {
        enforceManagerOrAdmin();
        return reportingClient.trend(metric, period, parseDate(from, "from"), parseDate(to, "to"), teamId);
    }

    public ReportDtos.PositivityByPersonResponse positivityByPerson(String periodFrom,
                                                                    String periodTo,
                                                                    String departmentId,
                                                                    String teamId,
                                                                    int limit,
                                                                    String sort) {
        enforceManagerOrAdmin();
        return reportingClient.positivityByPerson(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            limit,
            sort
        );
    }

    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(String periodFrom,
                                                                        String periodTo,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        String categoryId,
                                                                        int limit,
                                                                        String sort) {
        enforceManagerOrAdmin();
        return reportingClient.subcategoryFrequency(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            categoryId,
            limit,
            sort
        );
    }

    public ReportDtos.DashboardResponse dashboard(String periodFrom,
                                                  String periodTo,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId) {
        enforceManagerOrAdmin();
        return reportingClient.dashboard(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            personId
        );
    }

    public ReportDtos.TopSubcategoriesInsightsResponse topSubcategories(String periodFrom,
                                                                        String periodTo,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        int limit) {
        enforceManagerOrAdmin();
        return reportingClient.topSubcategories(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            limit
        );
    }

    private void enforceManagerOrAdmin() {
        var user = currentUserService.getCurrentUser();
        if (!user.isAdmin() && !user.isManager()) {
            throw new ApiException("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN);
        }
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail(field, "must be ISO date YYYY-MM-DD")));
        }
    }
}

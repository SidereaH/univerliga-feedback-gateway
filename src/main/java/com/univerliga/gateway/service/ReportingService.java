package com.univerliga.gateway.service;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.security.CurrentUser;
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

    public ReportDtos.SummaryResponse summary(String periodFrom,
                                              String periodTo,
                                              String departmentId,
                                              String teamId,
                                              String personId) {
        enforceReportsAccess();
        return reportingClient.summary(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            personId
        );
    }

    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(String periodFrom,
                                                                  String periodTo,
                                                                  String departmentId,
                                                                  String teamId,
                                                                  String personId) {
        enforceReportsAccess();
        return reportingClient.ratingsByCategory(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            personId
        );
    }

    public ReportDtos.TrendResponse trend(String metric,
                                          String granularity,
                                          String from,
                                          String to,
                                          String departmentId,
                                          String teamId,
                                          String personId) {
        enforceReportsAccess();
        validateTrendMetric(metric);
        validateGranularity(granularity);
        return reportingClient.trend(
            metric,
            granularity,
            parseDate(from, "from"),
            parseDate(to, "to"),
            departmentId,
            teamId,
            personId
        );
    }

    public ReportDtos.PositivityByPersonResponse positivityByPerson(String periodFrom,
                                                                    String periodTo,
                                                                    String departmentId,
                                                                    String teamId,
                                                                    int limit,
                                                                    String sort) {
        enforceReportsAccess();
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
                                                                        String personId,
                                                                        String categoryId,
                                                                        int limit,
                                                                        String sort) {
        enforceReportsAccess();
        return reportingClient.subcategoryFrequency(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            personId,
            categoryId,
            limit,
            sort
        );
    }

    public ReportDtos.TopTagsResponse topTags(String periodFrom,
                                              String periodTo,
                                              String departmentId,
                                              String teamId,
                                              int limit) {
        enforceReportsAccess();
        return reportingClient.topTags(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            limit
        );
    }

    public ReportDtos.DashboardResponse dashboard(String periodFrom,
                                                  String periodTo,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId) {
        enforceReportsAccess();
        return reportingClient.dashboard(
            parseDate(periodFrom, "periodFrom"),
            parseDate(periodTo, "periodTo"),
            departmentId,
            teamId,
            personId
        );
    }

    private void enforceReportsAccess() {
        CurrentUser user = currentUserService.getCurrentUser();
        if (!user.isAdmin() && !user.isManager() && !user.isHr()) {
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

    private void validateTrendMetric(String metric) {
        if (!List.of("responses", "avgRating", "positiveShare", "negativeShare").contains(metric)) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail("metric", "must be one of responses|avgRating|positiveShare|negativeShare")));
        }
    }

    private void validateGranularity(String granularity) {
        if (!List.of("month", "week").contains(granularity)) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail("granularity", "must be month or week")));
        }
    }
}

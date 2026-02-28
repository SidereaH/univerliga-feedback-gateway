package com.univerliga.gateway.client;

import com.univerliga.gateway.dto.ReportDtos;

import java.time.LocalDate;

public interface ReportingClient {
    ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId);

    ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from, LocalDate to, String teamId);

    ReportDtos.TrendResponse trend(String metric, String period, LocalDate from, LocalDate to, String teamId);

    ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from,
                                                             LocalDate to,
                                                             String departmentId,
                                                             String teamId,
                                                             int limit,
                                                             String sort);

    ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from,
                                                                 LocalDate to,
                                                                 String departmentId,
                                                                 String teamId,
                                                                 String categoryId,
                                                                 int limit,
                                                                 String sort);

    ReportDtos.DashboardResponse dashboard(LocalDate from,
                                           LocalDate to,
                                           String departmentId,
                                           String teamId,
                                           String personId);

    ReportDtos.TopSubcategoriesInsightsResponse topSubcategories(LocalDate from,
                                                                 LocalDate to,
                                                                 String departmentId,
                                                                 String teamId,
                                                                 int limit);
}

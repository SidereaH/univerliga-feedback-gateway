package com.univerliga.gateway.client;

import com.univerliga.gateway.dto.ReportDtos;

import java.time.LocalDate;

public interface ReportingClient {
    ReportDtos.SummaryResponse summary(LocalDate from,
                                       LocalDate to,
                                       String departmentId,
                                       String teamId,
                                       String personId);

    ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from,
                                                           LocalDate to,
                                                           String departmentId,
                                                           String teamId,
                                                           String personId);

    ReportDtos.TrendResponse trend(String metric,
                                   String granularity,
                                   LocalDate from,
                                   LocalDate to,
                                   String departmentId,
                                   String teamId,
                                   String personId);

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
                                                                 String personId,
                                                                 String categoryId,
                                                                 int limit,
                                                                 String sort);

    ReportDtos.TopTagsResponse topTags(LocalDate from,
                                       LocalDate to,
                                       String departmentId,
                                       String teamId,
                                       int limit);

    ReportDtos.DashboardResponse dashboard(LocalDate from,
                                           LocalDate to,
                                           String departmentId,
                                           String teamId,
                                           String personId);
}

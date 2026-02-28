package com.univerliga.gateway.service;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.security.CurrentUser;
import com.univerliga.gateway.security.CurrentUserService;
import com.univerliga.gateway.security.SecurityRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportingClient reportingClient;

    @Mock
    private CurrentUserService currentUserService;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(reportingClient, currentUserService);
    }

    @Test
    void employeeCannotReadReports() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", java.util.Set.of(SecurityRoles.EMPLOYEE)));

        ApiException ex = assertThrows(ApiException.class,
            () -> reportingService.summary("2026-01-01", "2026-01-31", null, null));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void managerCanReadReports() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));
        ReportDtos.SummaryResponse expected = new ReportDtos.SummaryResponse(
            new ReportDtos.ReportPeriod("2026-01-01", "2026-01-31"),
            new ReportDtos.Kpis(10, 4.0, 0.7)
        );
        when(reportingClient.summary(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1"))
            .thenReturn(expected);

        ReportDtos.SummaryResponse result = reportingService.summary("2026-01-01", "2026-01-31", "d_1", "t_1");

        assertEquals(10, result.kpis().responses());
        verify(reportingClient).summary(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1");
    }

    @Test
    void invalidDateReturnsValidationError() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));

        ApiException ex = assertThrows(ApiException.class,
            () -> reportingService.dashboard("bad", "2026-01-31", null, null, null));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void managerCanReadTopSubcategories() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));
        ReportDtos.TopSubcategoriesInsightsResponse expected = new ReportDtos.TopSubcategoriesInsightsResponse(
            new ReportDtos.ReportPeriod("2026-01-01", "2026-01-31"),
            java.util.List.of(),
            java.util.List.of()
        );
        when(reportingClient.topSubcategories(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", 5))
            .thenReturn(expected);

        ReportDtos.TopSubcategoriesInsightsResponse result =
            reportingService.topSubcategories("2026-01-01", "2026-01-31", "d_1", "t_1", 5);

        assertEquals("2026-01-01", result.period().from());
        verify(reportingClient).topSubcategories(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", 5);
    }
}

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
import java.util.List;
import java.util.Set;

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
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", Set.of(SecurityRoles.EMPLOYEE)));

        ApiException ex = assertThrows(ApiException.class,
            () -> reportingService.summary("2026-01-01", "2026-01-31", null, null, null));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void hrCanReadReports() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("hr", "p_hr", Set.of(SecurityRoles.HR)));
        ReportDtos.SummaryResponse expected = new ReportDtos.SummaryResponse(
            new ReportDtos.ReportPeriod("2026-01-01", "2026-01-31"),
            new ReportDtos.ScopeWithPersonDto("d_1", "t_1", null),
            new ReportDtos.SummaryKpis(10, 7, 3, 4, 5, 4.0, 0.7, 0.3, new ReportDtos.CoverageKpi(5, 8, 0.63))
        );
        when(reportingClient.summary(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", null))
            .thenReturn(expected);

        ReportDtos.SummaryResponse result = reportingService.summary("2026-01-01", "2026-01-31", "d_1", "t_1", null);

        assertEquals(10, result.kpis().responses());
        verify(reportingClient).summary(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", null);
    }

    @Test
    void invalidDateReturnsValidationError() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", Set.of(SecurityRoles.MANAGER)));

        ApiException ex = assertThrows(ApiException.class,
            () -> reportingService.dashboard("bad", "2026-01-31", null, null, null));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void managerCanReadTopTags() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", Set.of(SecurityRoles.MANAGER)));
        ReportDtos.TopTagsResponse expected = new ReportDtos.TopTagsResponse(
            new ReportDtos.ReportPeriod("2026-01-01", "2026-01-31"),
            List.of(),
            List.of()
        );
        when(reportingClient.topTags(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", 5))
            .thenReturn(expected);

        ReportDtos.TopTagsResponse result =
            reportingService.topTags("2026-01-01", "2026-01-31", "d_1", "t_1", 5);

        assertEquals("2026-01-01", result.period().from());
        verify(reportingClient).topTags(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "d_1", "t_1", 5);
    }
}

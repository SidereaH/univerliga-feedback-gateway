package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.dto.PersonDtos;
import com.univerliga.gateway.dto.TaskDtos;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import com.univerliga.gateway.security.CurrentUser;
import com.univerliga.gateway.security.CurrentUserService;
import com.univerliga.gateway.security.SecurityRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmServiceTest {

    @Mock
    private CrmClient crmClient;

    @Mock
    private CurrentUserService currentUserService;

    private CrmService crmService;

    @BeforeEach
    void setUp() {
        crmService = new CrmService(crmClient, currentUserService);
    }

    @Test
    void peopleForEmployeeReturnsOnlySelf() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", List.of(SecurityRoles.EMPLOYEE).stream().collect(java.util.stream.Collectors.toSet())));
        when(crmClient.findPeople(null, null, null)).thenReturn(List.of(
            person("p_employee", "kc_employee"),
            person("p_other", "kc_other")
        ));

        PersonDtos.PeoplePage page = crmService.people(null, null, null, 1, 20);

        assertEquals(1, page.items().size());
        assertEquals("p_employee", page.items().get(0).id());
    }

    @Test
    void personByIdForAdminContainsKeycloakId() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("admin", "p_admin", java.util.Set.of(SecurityRoles.ADMIN)));
        when(crmClient.findPersonById("p_employee")).thenReturn(Optional.of(person("p_employee", "kc_employee")));

        PersonDtos.PersonDetails response = crmService.personById("p_employee");

        assertEquals("kc_employee", response.keycloakUserId());
    }

    @Test
    void personByIdForEmployeeOtherPersonIsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", java.util.Set.of(SecurityRoles.EMPLOYEE)));

        ApiException ex = assertThrows(ApiException.class, () -> crmService.personById("p_other"));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void tasksInvalidDateThrowsValidationError() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("admin", "p_admin", java.util.Set.of(SecurityRoles.ADMIN)));

        ApiException ex = assertThrows(ApiException.class,
            () -> crmService.tasks(null, null, null, "bad-date", null, 1, 20));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void createTaskForbiddenForEmployee() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", java.util.Set.of(SecurityRoles.EMPLOYEE)));

        TaskDtos.CreateTaskRequest request = new TaskDtos.CreateTaskRequest(
            "Title", "Desc", new TaskDtos.PeriodDto("2026-01-01", "2026-01-31"), "p_manager", "p_employee", List.of("p_employee")
        );

        ApiException ex = assertThrows(ApiException.class, () -> crmService.createTask(request));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void closeTaskReturnsClosedTimestamp() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task("task_1", List.of("p_employee"), null)));
        when(crmClient.closeTask("task_1")).thenReturn(task("task_1", List.of("p_employee"), Instant.now()));

        TaskDtos.CloseTaskResponse response = crmService.closeTask("task_1");

        assertEquals("CLOSED", response.status());
        assertNotNull(response.closedAt());
        verify(crmClient).closeTask("task_1");
    }

    private PersonRecord person(String id, String kcId) {
        return new PersonRecord(id, "Name", "mail@example.com", "d_1", "t_1", true, "PROVISIONED", kcId, Instant.now(), "EMPLOYEE");
    }

    private TaskRecord task(String id, List<String> participants, Instant closedAt) {
        return new TaskRecord(id, "Title", "Desc", "CLOSED", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "p_manager", "p_employee", participants, Instant.now(), closedAt);
    }
}

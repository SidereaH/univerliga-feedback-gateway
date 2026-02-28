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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CrmService {
    private final CrmClient crmClient;
    private final CurrentUserService currentUserService;

    public CrmService(CrmClient crmClient, CurrentUserService currentUserService) {
        this.crmClient = crmClient;
        this.currentUserService = currentUserService;
    }

    public PersonDtos.PeoplePage people(String query, String departmentId, String teamId, int page, int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        List<PersonRecord> filtered = crmClient.findPeople(query, departmentId, teamId);
        if (user.isEmployee() && !user.isAdmin() && !user.isManager()) {
            filtered = filtered.stream().filter(p -> p.id().equals(user.personId())).toList();
        }
        List<PersonDtos.PersonSummary> items = PaginationUtils.slice(filtered, page, size).stream().map(this::toPersonSummary).toList();
        return new PersonDtos.PeoplePage(items, PaginationUtils.page(filtered, page, size));
    }

    public PersonDtos.PersonDetails personById(String personId) {
        CurrentUser user = currentUserService.getCurrentUser();
        if (user.isEmployee() && !user.personId().equals(personId) && !user.isManager() && !user.isAdmin()) {
            throw forbidden();
        }
        PersonRecord person = crmClient.findPersonById(personId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Person not found", HttpStatus.NOT_FOUND));
        String keycloakUserId = user.isAdmin() ? person.keycloakUserId() : null;
        return new PersonDtos.PersonDetails(person.id(), person.displayName(), person.email(), person.departmentId(), person.teamId(),
            person.active(), person.createdAt().toString(), person.identityStatus(), keycloakUserId);
    }

    public PersonDtos.PersonDetails createPerson(PersonDtos.CreatePersonRequest request) {
        PersonRecord person = crmClient.createPerson(request.displayName(), request.email(), request.departmentId(), request.teamId(), request.role());
        return new PersonDtos.PersonDetails(person.id(), person.displayName(), person.email(), person.departmentId(), person.teamId(),
            person.active(), person.createdAt().toString(), person.identityStatus(), null);
    }

    public PersonDtos.PersonDetails patchPerson(String personId, PersonDtos.PatchPersonRequest request) {
        requireRole(SecurityRoles.ADMIN);
        crmClient.findPersonById(personId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Person not found", HttpStatus.NOT_FOUND));
        PersonRecord person = crmClient.patchPerson(personId, request.displayName(), request.email(), request.departmentId(), request.teamId(), request.active());
        return new PersonDtos.PersonDetails(person.id(), person.displayName(), person.email(), person.departmentId(), person.teamId(),
            person.active(), person.createdAt().toString(), person.identityStatus(), person.keycloakUserId());
    }

    public PersonDtos.DeleteResult deletePerson(String personId) {
        requireRole(SecurityRoles.ADMIN);
        boolean deleted = crmClient.deletePerson(personId);
        if (!deleted) {
            throw new ApiException("NOT_FOUND", "Person not found", HttpStatus.NOT_FOUND);
        }
        return new PersonDtos.DeleteResult(true);
    }

    public TaskDtos.TaskPage tasks(String status,
                                   String assigneeId,
                                   String participantId,
                                   String periodFrom,
                                   String periodTo,
                                   int page,
                                   int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        LocalDate from = parseDate(periodFrom, "periodFrom");
        LocalDate to = parseDate(periodTo, "periodTo");

        List<TaskRecord> tasks = crmClient.findTasks(status, assigneeId, participantId, from, to);
        if (user.isEmployee() && !user.isAdmin() && !user.isManager()) {
            tasks = tasks.stream().filter(t -> t.participantIds().contains(user.personId())).toList();
        }
        List<TaskDtos.TaskResponse> items = PaginationUtils.slice(tasks, page, size).stream().map(this::toTask).toList();
        return new TaskDtos.TaskPage(items, PaginationUtils.page(tasks, page, size));
    }

    public TaskDtos.TaskResponse taskById(String taskId) {
        CurrentUser user = currentUserService.getCurrentUser();
        TaskRecord task = crmClient.findTaskById(taskId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND));
        if (user.isEmployee() && !user.isManager() && !user.isAdmin() && !task.participantIds().contains(user.personId())) {
            throw forbidden();
        }
        return toTask(task);
    }

    public TaskDtos.TaskResponse createTask(TaskDtos.CreateTaskRequest request) {
        requireManagerOrAdmin();
        TaskRecord task = crmClient.createTask(request.title(), request.description(),
            LocalDate.parse(request.period().from()), LocalDate.parse(request.period().to()),
            request.ownerId(), request.assigneeId(), request.participantIds());
        return toTask(task);
    }

    public TaskDtos.TaskResponse patchTask(String taskId, TaskDtos.PatchTaskRequest request) {
        requireManagerOrAdmin();
        crmClient.findTaskById(taskId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND));
        TaskRecord task = crmClient.patchTask(taskId, request.title(), request.description(), request.status(),
            request.period() != null ? LocalDate.parse(request.period().from()) : null,
            request.period() != null ? LocalDate.parse(request.period().to()) : null,
            request.assigneeId(), request.participantIds());
        return toTask(task);
    }

    public TaskDtos.CloseTaskResponse closeTask(String taskId) {
        requireManagerOrAdmin();
        crmClient.findTaskById(taskId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND));
        TaskRecord closed = crmClient.closeTask(taskId);
        return new TaskDtos.CloseTaskResponse(closed.status(), closed.closedAt().toString());
    }

    private void requireRole(String role) {
        if (!currentUserService.getCurrentUser().hasRole(role)) {
            throw forbidden();
        }
    }

    private void requireManagerOrAdmin() {
        CurrentUser user = currentUserService.getCurrentUser();
        if (!user.isManager() && !user.isAdmin()) {
            throw forbidden();
        }
    }

    private ApiException forbidden() {
        return new ApiException("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN);
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new com.univerliga.gateway.error.ApiErrorDetail(field, "must be ISO date YYYY-MM-DD")));
        }
    }

    private PersonDtos.PersonSummary toPersonSummary(PersonRecord p) {
        return new PersonDtos.PersonSummary(p.id(), p.displayName(), p.email(), p.departmentId(), p.teamId(), p.active(), p.createdAt().toString());
    }

    private TaskDtos.TaskResponse toTask(TaskRecord t) {
        return new TaskDtos.TaskResponse(t.id(), t.title(), t.description(), t.status(),
            new TaskDtos.PeriodDto(t.periodFrom().toString(), t.periodTo().toString()), t.ownerId(), t.assigneeId(),
            t.participantIds(), t.createdAt().toString());
    }
}

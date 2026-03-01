package com.univerliga.gateway.controller;

import com.univerliga.gateway.dto.ApiEnvelope;
import com.univerliga.gateway.dto.PersonDtos;
import com.univerliga.gateway.dto.TaskDtos;
import com.univerliga.gateway.service.ApiResponseFactory;
import com.univerliga.gateway.service.CrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/crm", produces = "application/json;charset=UTF-8")
@Tag(name = "CRM", description = "People and tasks endpoints")
public class CrmController {
    private final CrmService crmService;
    private final ApiResponseFactory responseFactory;

    public CrmController(CrmService crmService, ApiResponseFactory responseFactory) {
        this.crmService = crmService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/people")
    @Operation(summary = "Search people", description = "Returns paginated list of people with optional filters (ADMIN/MANAGER only)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<PersonDtos.PeoplePage> people(@Parameter(description = "Text query by name/email") @RequestParam(required = false) String query,
                                                     @Parameter(description = "Department filter") @RequestParam(required = false) String departmentId,
                                                     @Parameter(description = "Team filter") @RequestParam(required = false) String teamId,
                                                     @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                     @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(crmService.people(query, departmentId, teamId, page, size));
    }

    @PostMapping(value = "/people", consumes = "application/json")
    @Operation(summary = "Create person", description = "Creates person in CRM")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiEnvelope<PersonDtos.PersonDetails> createPerson(@RequestBody @Valid PersonDtos.CreatePersonRequest request) {
        return responseFactory.ok(crmService.createPerson(request));
    }

    @GetMapping("/people/{personId}")
    @Operation(summary = "Get person", description = "Returns person details by id")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<PersonDtos.PersonDetails> person(@Parameter(description = "Person identifier") @PathVariable String personId) {
        return responseFactory.ok(crmService.personById(personId));
    }

    @PatchMapping(value = "/people/{personId}", consumes = "application/json")
    @Operation(summary = "Patch person", description = "Partially updates person data")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiEnvelope<PersonDtos.PersonDetails> patchPerson(@Parameter(description = "Person identifier") @PathVariable String personId,
                                                             @RequestBody @Valid PersonDtos.PatchPersonRequest request) {
        return responseFactory.ok(crmService.patchPerson(personId, request));
    }

    @DeleteMapping("/people/{personId}")
    @Operation(summary = "Delete person", description = "Deletes person by id")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiEnvelope<PersonDtos.DeleteResult> deletePerson(@Parameter(description = "Person identifier") @PathVariable String personId) {
        return responseFactory.ok(crmService.deletePerson(personId));
    }

    @GetMapping("/tasks")
    @Operation(summary = "Search tasks", description = "Returns paginated list of tasks with filters")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<TaskDtos.TaskPage> tasks(@Parameter(description = "Task status filter") @RequestParam(required = false) String status,
                                                @Parameter(description = "Assignee person id filter") @RequestParam(required = false) String assigneeId,
                                                @Parameter(description = "Participant person id filter") @RequestParam(required = false) String participantId,
                                                @Parameter(description = "Period start filter (YYYY-MM-DD)") @RequestParam(required = false) String periodFrom,
                                                @Parameter(description = "Period end filter (YYYY-MM-DD)") @RequestParam(required = false) String periodTo,
                                                @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
                                                @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return responseFactory.ok(crmService.tasks(status, assigneeId, participantId, periodFrom, periodTo, page, size));
    }

    @PostMapping(value = "/tasks", consumes = "application/json")
    @Operation(summary = "Create task", description = "Creates new task")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<TaskDtos.TaskResponse> createTask(@RequestBody @Valid TaskDtos.CreateTaskRequest request) {
        return responseFactory.ok(crmService.createTask(request));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get task", description = "Returns task details by id")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ApiEnvelope<TaskDtos.TaskResponse> task(@Parameter(description = "Task identifier") @PathVariable String taskId) {
        return responseFactory.ok(crmService.taskById(taskId));
    }

    @PatchMapping(value = "/tasks/{taskId}", consumes = "application/json")
    @Operation(summary = "Patch task", description = "Partially updates task")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<TaskDtos.TaskResponse> patchTask(@Parameter(description = "Task identifier") @PathVariable String taskId,
                                                        @RequestBody @Valid TaskDtos.PatchTaskRequest request) {
        return responseFactory.ok(crmService.patchTask(taskId, request));
    }

    @PostMapping("/tasks/{taskId}/close")
    @Operation(summary = "Close task", description = "Closes task and returns close timestamp")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiEnvelope<TaskDtos.CloseTaskResponse> closeTask(@Parameter(description = "Task identifier") @PathVariable String taskId) {
        return responseFactory.ok(crmService.closeTask(taskId));
    }
}

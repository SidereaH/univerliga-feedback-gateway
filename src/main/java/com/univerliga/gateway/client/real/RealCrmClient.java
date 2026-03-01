package com.univerliga.gateway.client.real;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealCrmClient implements CrmClient {
    private static final ParameterizedTypeReference<Envelope<PagedResult<CrmPersonDto>>> PEOPLE_PAGE_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<CrmPersonDto>> PERSON_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<DeleteResultDto>> DELETE_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<PagedResult<CrmTaskDto>>> TASKS_PAGE_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<CrmTaskDto>> TASK_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealCrmClient(@Qualifier("crmRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<PersonRecord> findPeople(String query, String departmentId, String teamId) {
        List<CrmPersonDto> all = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            PagedResult<CrmPersonDto> result = getPeoplePage(query, departmentId, teamId, page, 200);
            all.addAll(result.items());
            totalPages = Math.max(result.page().totalPages(), 1);
            page++;
        } while (page <= totalPages);
        return all.stream().map(this::toPersonRecord).toList();
    }

    @Override
    public Optional<PersonRecord> findPersonById(String personId) {
        try {
            CrmPersonDto person = requireData(restClient.get()
                .uri("/api/v1/crm/people/{personId}", personId)
                .retrieve()
                .body(PERSON_TYPE));
            return Optional.of(toPersonRecord(person));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public PersonRecord createPerson(String displayName, String email, String departmentId, String teamId, String role) {
        try {
            CrmPersonDto person = requireData(restClient.post()
                .uri("/api/v1/crm/people")
                .body(new CreatePersonRequest(displayName, email, departmentId, teamId, role))
                .retrieve()
                .body(PERSON_TYPE));
            return toPersonRecord(person);
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public PersonRecord patchPerson(String personId, String displayName, String email, String departmentId, String teamId, Boolean active) {
        try {
            CrmPersonDto person = requireData(restClient.patch()
                .uri("/api/v1/crm/people/{personId}", personId)
                .body(new PatchPersonRequest(displayName, email, departmentId, teamId, active))
                .retrieve()
                .body(PERSON_TYPE));
            return toPersonRecord(person);
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public boolean deletePerson(String personId) {
        try {
            DeleteResultDto result = requireData(restClient.delete()
                .uri("/api/v1/crm/people/{personId}", personId)
                .retrieve()
                .body(DELETE_TYPE));
            return result.deleted();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public List<TaskRecord> findTasks(String status, String assigneeId, String participantId, LocalDate from, LocalDate to) {
        List<CrmTaskDto> all = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            PagedResult<CrmTaskDto> result = getTasksPage(status, assigneeId, participantId, from, to, page, 200);
            all.addAll(result.items());
            totalPages = Math.max(result.page().totalPages(), 1);
            page++;
        } while (page <= totalPages);
        return all.stream().map(this::toTaskRecord).toList();
    }

    @Override
    public Optional<TaskRecord> findTaskById(String taskId) {
        try {
            CrmTaskDto task = requireData(restClient.get()
                .uri("/api/v1/crm/tasks/{taskId}", taskId)
                .retrieve()
                .body(TASK_TYPE));
            return Optional.of(toTaskRecord(task));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public TaskRecord createTask(String title, String description, LocalDate from, LocalDate to, String ownerId, String assigneeId, List<String> participantIds) {
        try {
            CrmTaskDto task = requireData(restClient.post()
                .uri("/api/v1/crm/tasks")
                .body(new CreateTaskRequest(title, description, new PeriodDto(from, to), ownerId, assigneeId, participantIds))
                .retrieve()
                .body(TASK_TYPE));
            return toTaskRecord(task);
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public TaskRecord patchTask(String taskId, String title, String description, String status, LocalDate from, LocalDate to, String assigneeId, List<String> participantIds) {
        try {
            CrmTaskDto task = requireData(restClient.patch()
                .uri("/api/v1/crm/tasks/{taskId}", taskId)
                .body(new PatchTaskRequest(title, description, status, from != null && to != null ? new PeriodDto(from, to) : null, assigneeId, participantIds))
                .retrieve()
                .body(TASK_TYPE));
            return toTaskRecord(task);
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public TaskRecord closeTask(String taskId) {
        try {
            restClient.post()
                .uri("/api/v1/crm/tasks/{taskId}/close", taskId)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
        return findTaskById(taskId).orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found after close", HttpStatus.NOT_FOUND));
    }

    private PagedResult<CrmPersonDto> getPeoplePage(String query, String departmentId, String teamId, int page, int size) {
        try {
            return requireData(restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/crm/people")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParamIfPresent("query", Optional.ofNullable(query))
                    .queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
                    .queryParamIfPresent("teamId", Optional.ofNullable(teamId))
                    .build())
                .retrieve()
                .body(PEOPLE_PAGE_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private PagedResult<CrmTaskDto> getTasksPage(String status,
                                                 String assigneeId,
                                                 String participantId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 int page,
                                                 int size) {
        try {
            return requireData(restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/crm/tasks")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParamIfPresent("status", Optional.ofNullable(status))
                    .queryParamIfPresent("assigneeId", Optional.ofNullable(assigneeId))
                    .queryParamIfPresent("participantId", Optional.ofNullable(participantId))
                    .queryParamIfPresent("periodFrom", Optional.ofNullable(from).map(LocalDate::toString))
                    .queryParamIfPresent("periodTo", Optional.ofNullable(to).map(LocalDate::toString))
                    .build())
                .retrieve()
                .body(TASKS_PAGE_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private PersonRecord toPersonRecord(CrmPersonDto dto) {
        return new PersonRecord(
            dto.id(),
            dto.displayName(),
            dto.email(),
            dto.departmentId(),
            dto.teamId(),
            dto.active(),
            dto.identityStatus(),
            dto.keycloakUserId(),
            parseInstant(dto.createdAt()),
            dto.role()
        );
    }

    private TaskRecord toTaskRecord(CrmTaskDto dto) {
        return new TaskRecord(
            dto.id(),
            dto.title(),
            dto.description(),
            dto.status(),
            dto.period().from(),
            dto.period().to(),
            dto.ownerId(),
            dto.assigneeId(),
            dto.participantIds() == null ? List.of() : dto.participantIds(),
            parseInstant(dto.createdAt()),
            dto.closedAt() == null ? null : parseInstant(dto.closedAt())
        );
    }

    private <T> T requireData(Envelope<T> envelope) {
        if (envelope == null || envelope.data() == null) {
            throw new ApiException("BAD_GATEWAY", "CRM response is empty", HttpStatus.BAD_GATEWAY);
        }
        return envelope.data();
    }

    private Instant parseInstant(String value) {
        return value == null ? Instant.now() : OffsetDateTime.parse(value).toInstant();
    }

    private ApiException toApiException(RestClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            JsonNode error = body.path("error");
            String code = error.path("code").asText();
            String message = error.path("message").asText();
            if (!code.isBlank() && !message.isBlank()) {
                return new ApiException(code, message, status, extractDetails(error.path("details")));
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return new ApiException("DOWNSTREAM_ERROR", "CRM request failed", status);
    }

    private List<ApiErrorDetail> extractDetails(JsonNode detailsNode) {
        if (!detailsNode.isArray()) {
            return List.of();
        }
        List<ApiErrorDetail> details = new ArrayList<>();
        for (JsonNode node : detailsNode) {
            String field = node.path("field").asText("");
            String issue = node.path("issue").asText(node.path("message").asText(""));
            if (!field.isBlank() || !issue.isBlank()) {
                details.add(new ApiErrorDetail(field, issue));
            }
        }
        return details;
    }

    private ApiException serviceUnavailable(RestClientException ex) {
        return new ApiException("DOWNSTREAM_UNAVAILABLE", "CRM service unavailable", HttpStatus.BAD_GATEWAY,
            List.of(new ApiErrorDetail("crm", ex.getMessage())));
    }

    private record Envelope<T>(T data) {
    }

    private record PagedResult<T>(List<T> items, PageMeta page) {
    }

    private record PageMeta(int page, int size, long totalItems, int totalPages) {
    }

    private record PeriodDto(LocalDate from, LocalDate to) {
    }

    private record CrmPersonDto(String id,
                                String displayName,
                                String email,
                                String departmentId,
                                String teamId,
                                String role,
                                boolean active,
                                String identityStatus,
                                String keycloakUserId,
                                String createdAt) {
    }

    private record CrmTaskDto(String id,
                              String title,
                              String description,
                              String status,
                              PeriodDto period,
                              String ownerId,
                              String assigneeId,
                              List<String> participantIds,
                              String createdAt,
                              String closedAt) {
    }

    private record DeleteResultDto(boolean deleted) {
    }

    private record CreatePersonRequest(String displayName, String email, String departmentId, String teamId, String role) {
    }

    private record PatchPersonRequest(String displayName, String email, String departmentId, String teamId, Boolean active) {
    }

    private record CreateTaskRequest(String title,
                                     String description,
                                     PeriodDto period,
                                     String ownerId,
                                     String assigneeId,
                                     List<String> participantIds) {
    }

    private record PatchTaskRequest(String title,
                                    String description,
                                    String status,
                                    PeriodDto period,
                                    String assigneeId,
                                    List<String> participantIds) {
    }
}

package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockCrmClient implements CrmClient {
    private final MockDataStore dataStore;

    public MockCrmClient(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public List<PersonRecord> findPeople(String query, String departmentId, String teamId) {
        return dataStore.people().stream()
            .filter(p -> query == null || (p.displayName() + p.email()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
            .filter(p -> departmentId == null || departmentId.equals(p.departmentId()))
            .filter(p -> teamId == null || teamId.equals(p.teamId()))
            .sorted(Comparator.comparing(PersonRecord::id))
            .toList();
    }

    @Override
    public Optional<PersonRecord> findPersonById(String personId) {
        return dataStore.people().stream().filter(p -> p.id().equals(personId)).findFirst();
    }

    @Override
    public PersonRecord createPerson(String displayName, String email, String departmentId, String teamId, String role) {
        String id = "p_" + UUID.randomUUID().toString().substring(0, 8);
        PersonRecord record = new PersonRecord(id, displayName, email, departmentId, teamId, true,
            "PENDING", null, Instant.now(), role);
        dataStore.people().add(record);
        return record;
    }

    @Override
    public PersonRecord patchPerson(String personId, String displayName, String email, String departmentId, String teamId, Boolean active) {
        PersonRecord existing = findPersonById(personId).orElseThrow();
        PersonRecord updated = new PersonRecord(
            existing.id(),
            displayName != null ? displayName : existing.displayName(),
            email != null ? email : existing.email(),
            departmentId != null ? departmentId : existing.departmentId(),
            teamId != null ? teamId : existing.teamId(),
            active != null ? active : existing.active(),
            existing.identityStatus(),
            existing.keycloakUserId(),
            existing.createdAt(),
            existing.role()
        );
        dataStore.people().remove(existing);
        dataStore.people().add(updated);
        return updated;
    }

    @Override
    public boolean deletePerson(String personId) {
        return dataStore.people().removeIf(p -> p.id().equals(personId));
    }

    @Override
    public List<TaskRecord> findTasks(String status, String assigneeId, String participantId, LocalDate from, LocalDate to) {
        return dataStore.tasks().stream()
            .filter(t -> status == null || status.equals(t.status()))
            .filter(t -> assigneeId == null || assigneeId.equals(t.assigneeId()))
            .filter(t -> participantId == null || t.participantIds().contains(participantId))
            .filter(t -> from == null || !t.periodTo().isBefore(from))
            .filter(t -> to == null || !t.periodFrom().isAfter(to))
            .sorted(Comparator.comparing(TaskRecord::id))
            .toList();
    }

    @Override
    public Optional<TaskRecord> findTaskById(String taskId) {
        return dataStore.tasks().stream().filter(t -> t.id().equals(taskId)).findFirst();
    }

    @Override
    public TaskRecord createTask(String title, String description, LocalDate from, LocalDate to, String ownerId, String assigneeId, List<String> participantIds) {
        String id = "task_" + UUID.randomUUID().toString().substring(0, 8);
        TaskRecord record = new TaskRecord(id, title, description, "DRAFT", from, to, ownerId, assigneeId, participantIds, Instant.now(), null);
        dataStore.tasks().add(record);
        return record;
    }

    @Override
    public TaskRecord patchTask(String taskId, String title, String description, String status, LocalDate from, LocalDate to, String assigneeId, List<String> participantIds) {
        TaskRecord existing = findTaskById(taskId).orElseThrow();
        TaskRecord updated = new TaskRecord(
            existing.id(),
            title != null ? title : existing.title(),
            description != null ? description : existing.description(),
            status != null ? status : existing.status(),
            from != null ? from : existing.periodFrom(),
            to != null ? to : existing.periodTo(),
            existing.ownerId(),
            assigneeId != null ? assigneeId : existing.assigneeId(),
            participantIds != null && !participantIds.isEmpty() ? participantIds : existing.participantIds(),
            existing.createdAt(),
            existing.closedAt()
        );
        dataStore.tasks().remove(existing);
        dataStore.tasks().add(updated);
        return updated;
    }

    @Override
    public TaskRecord closeTask(String taskId) {
        TaskRecord existing = findTaskById(taskId).orElseThrow();
        TaskRecord updated = new TaskRecord(
            existing.id(), existing.title(), existing.description(), "CLOSED", existing.periodFrom(),
            existing.periodTo(), existing.ownerId(), existing.assigneeId(), existing.participantIds(),
            existing.createdAt(), Instant.now()
        );
        dataStore.tasks().remove(existing);
        dataStore.tasks().add(updated);
        return updated;
    }
}

package com.univerliga.gateway.client.real;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealCrmClient implements CrmClient {

    @Override
    public List<PersonRecord> findPeople(String query, String departmentId, String teamId) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public Optional<PersonRecord> findPersonById(String personId) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public PersonRecord createPerson(String displayName, String email, String departmentId, String teamId, String role) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public PersonRecord patchPerson(String personId, String displayName, String email, String departmentId, String teamId, Boolean active) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public boolean deletePerson(String personId) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public List<TaskRecord> findTasks(String status, String assigneeId, String participantId, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public Optional<TaskRecord> findTaskById(String taskId) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public TaskRecord createTask(String title, String description, LocalDate from, LocalDate to, String ownerId, String assigneeId, List<String> participantIds) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public TaskRecord patchTask(String taskId, String title, String description, String status, LocalDate from, LocalDate to, String assigneeId, List<String> participantIds) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }

    @Override
    public TaskRecord closeTask(String taskId) {
        throw new UnsupportedOperationException("TODO: integrate real CRM HTTP API");
    }
}

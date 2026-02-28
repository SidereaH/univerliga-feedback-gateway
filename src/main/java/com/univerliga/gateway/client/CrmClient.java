package com.univerliga.gateway.client;

import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CrmClient {
    List<PersonRecord> findPeople(String query, String departmentId, String teamId);

    Optional<PersonRecord> findPersonById(String personId);

    PersonRecord createPerson(String displayName, String email, String departmentId, String teamId, String role);

    PersonRecord patchPerson(String personId, String displayName, String email, String departmentId, String teamId, Boolean active);

    boolean deletePerson(String personId);

    List<TaskRecord> findTasks(String status, String assigneeId, String participantId, LocalDate from, LocalDate to);

    Optional<TaskRecord> findTaskById(String taskId);

    TaskRecord createTask(String title, String description, LocalDate from, LocalDate to, String ownerId, String assigneeId, List<String> participantIds);

    TaskRecord patchTask(String taskId, String title, String description, String status, LocalDate from, LocalDate to, String assigneeId, List<String> participantIds);

    TaskRecord closeTask(String taskId);
}

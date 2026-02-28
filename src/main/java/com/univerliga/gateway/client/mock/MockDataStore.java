package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MockDataStore {
    private final List<PersonRecord> people = new CopyOnWriteArrayList<>();
    private final List<TaskRecord> tasks = new CopyOnWriteArrayList<>();
    private final List<FeedbackRecord> feedback = new CopyOnWriteArrayList<>();
    private final List<CategoryRecord> categories = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!people.isEmpty()) {
            return;
        }
        people.addAll(List.of(
            new PersonRecord("p_admin", "Admin User", "admin@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_admin", Instant.now().minusSeconds(3600L * 24 * 300), "ADMIN"),
            new PersonRecord("p_manager", "Manager User", "manager@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_manager", Instant.now().minusSeconds(3600L * 24 * 280), "MANAGER"),
            new PersonRecord("p_employee", "Employee User", "employee@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_employee", Instant.now().minusSeconds(3600L * 24 * 270), "EMPLOYEE"),
            new PersonRecord("p_4", "Alice Stone", "alice@univerliga.local", "d_1", "t_2", true, "PROVISIONED", "kc_4", Instant.now().minusSeconds(3600L * 24 * 260), "EMPLOYEE"),
            new PersonRecord("p_5", "Bob Reed", "bob@univerliga.local", "d_1", "t_2", true, "PROVISIONED", "kc_5", Instant.now().minusSeconds(3600L * 24 * 250), "EMPLOYEE"),
            new PersonRecord("p_6", "Cara Finch", "cara@univerliga.local", "d_2", "t_3", true, "PENDING", null, Instant.now().minusSeconds(3600L * 24 * 240), "EMPLOYEE"),
            new PersonRecord("p_7", "Dan Morse", "dan@univerliga.local", "d_2", "t_3", true, "PROVISIONED", "kc_7", Instant.now().minusSeconds(3600L * 24 * 230), "EMPLOYEE"),
            new PersonRecord("p_8", "Eve Larson", "eve@univerliga.local", "d_3", "t_4", true, "PROVISIONED", "kc_8", Instant.now().minusSeconds(3600L * 24 * 220), "MANAGER"),
            new PersonRecord("p_9", "Finn Pratt", "finn@univerliga.local", "d_3", "t_4", false, "PENDING", null, Instant.now().minusSeconds(3600L * 24 * 210), "EMPLOYEE"),
            new PersonRecord("p_10", "Gina Wells", "gina@univerliga.local", "d_2", "t_3", true, "PROVISIONED", "kc_10", Instant.now().minusSeconds(3600L * 24 * 200), "EMPLOYEE")
        ));

        tasks.addAll(List.of(
            task("task_1", "Quarter review", "Review Q1 outcomes", "ACTIVE", "2026-01-01", "2026-01-31", "p_manager", "p_employee", List.of("p_employee", "p_4")),
            task("task_2", "Onboarding", "New employee onboarding", "ACTIVE", "2026-02-01", "2026-02-28", "p_manager", "p_5", List.of("p_5", "p_6")),
            task("task_3", "Culture survey", "Internal feedback cycle", "DRAFT", "2026-02-01", "2026-03-01", "p_admin", "p_7", List.of("p_7", "p_8")),
            task("task_4", "Performance sync", "Team performance check", "ACTIVE", "2026-01-15", "2026-02-15", "p_8", "p_10", List.of("p_10", "p_manager")),
            task("task_5", "Risk audit", "Audit monthly risks", "CLOSED", "2025-12-01", "2025-12-31", "p_admin", "p_manager", List.of("p_manager", "p_4")),
            task("task_6", "Hiring review", "Candidate assessment", "ACTIVE", "2026-02-10", "2026-03-10", "p_manager", "p_employee", List.of("p_employee", "p_9")),
            task("task_7", "Vendor check", "Evaluate new vendor", "DRAFT", "2026-03-01", "2026-03-31", "p_admin", "p_8", List.of("p_8", "p_5")),
            task("task_8", "Service KPI", "Monthly KPI sync", "ACTIVE", "2026-01-01", "2026-02-01", "p_8", "p_6", List.of("p_6", "p_7")),
            task("task_9", "Quality sprint", "Quality improvements", "ACTIVE", "2026-02-05", "2026-03-05", "p_manager", "p_4", List.of("p_4", "p_employee")),
            task("task_10", "Retention", "Retention plan", "CLOSED", "2025-11-01", "2025-11-30", "p_admin", "p_10", List.of("p_10", "p_7"))
        ));

        categories.addAll(List.of(
            new CategoryRecord("cat_1", "Performance", List.of(
                new CategoryRecord.SubcategoryRecord("sub_1", "Communication"),
                new CategoryRecord.SubcategoryRecord("sub_2", "Delivery")
            )),
            new CategoryRecord("cat_2", "Culture", List.of(
                new CategoryRecord.SubcategoryRecord("sub_3", "Teamwork"),
                new CategoryRecord.SubcategoryRecord("sub_4", "Initiative")
            ))
        ));

        for (int i = 1; i <= 30; i++) {
            feedback.add(new FeedbackRecord(
                "fb_" + i,
                "task_" + ((i % 10) + 1),
                i % 2 == 0 ? "p_employee" : "p_4",
                i % 3 == 0 ? "p_manager" : "p_employee",
                i % 2 == 0 ? "cat_1" : "cat_2",
                i % 2 == 0 ? "sub_1" : "sub_3",
                (i % 5) + 1,
                "Mock feedback #" + i,
                Instant.now().minusSeconds(i * 3600L)
            ));
        }
    }

    private TaskRecord task(String id,
                            String title,
                            String description,
                            String status,
                            String from,
                            String to,
                            String ownerId,
                            String assigneeId,
                            List<String> participants) {
        Instant createdAt = Instant.now().minusSeconds(3600L * 24 * 30);
        Instant closedAt = "CLOSED".equals(status) ? Instant.now().minusSeconds(3600L * 24) : null;
        return new TaskRecord(id, title, description, status, LocalDate.parse(from), LocalDate.parse(to), ownerId,
            assigneeId, participants, createdAt, closedAt);
    }

    public List<PersonRecord> people() {
        return people;
    }

    public List<TaskRecord> tasks() {
        return tasks;
    }

    public List<FeedbackRecord> feedback() {
        return feedback;
    }

    public List<CategoryRecord> categories() {
        return categories;
    }
}

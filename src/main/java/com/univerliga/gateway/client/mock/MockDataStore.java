package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.CategoryRecord.SubcategoryRecord.Polarity;
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
        initPeople();
        initTasks();
        initCategories();
        initFeedback();
    }

    private void initPeople() {
        people.addAll(List.of(
            new PersonRecord("p_admin", "Admin User", "admin@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_admin", Instant.now().minusSeconds(3600L * 24 * 300), "ADMIN"),
            new PersonRecord("p_manager", "Manager User", "manager@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_manager", Instant.now().minusSeconds(3600L * 24 * 280), "MANAGER"),
            new PersonRecord("p_employee", "Employee User", "employee@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_employee", Instant.now().minusSeconds(3600L * 24 * 270), "EMPLOYEE"),
            new PersonRecord("p_hr", "HR User", "hr@univerliga.local", "d_1", "t_1", true, "PROVISIONED", "kc_hr", Instant.now().minusSeconds(3600L * 24 * 265), "HR"),
            new PersonRecord("p_4", "Alice Stone", "alice@univerliga.local", "d_1", "t_2", true, "PROVISIONED", "kc_4", Instant.now().minusSeconds(3600L * 24 * 260), "EMPLOYEE"),
            new PersonRecord("p_5", "Bob Reed", "bob@univerliga.local", "d_1", "t_2", true, "PROVISIONED", "kc_5", Instant.now().minusSeconds(3600L * 24 * 250), "EMPLOYEE"),
            new PersonRecord("p_6", "Cara Finch", "cara@univerliga.local", "d_2", "t_3", true, "PENDING", null, Instant.now().minusSeconds(3600L * 24 * 240), "EMPLOYEE"),
            new PersonRecord("p_7", "Dan Morse", "dan@univerliga.local", "d_2", "t_3", true, "PROVISIONED", "kc_7", Instant.now().minusSeconds(3600L * 24 * 230), "EMPLOYEE"),
            new PersonRecord("p_8", "Eve Larson", "eve@univerliga.local", "d_3", "t_4", true, "PROVISIONED", "kc_8", Instant.now().minusSeconds(3600L * 24 * 220), "MANAGER"),
            new PersonRecord("p_9", "Finn Pratt", "finn@univerliga.local", "d_3", "t_4", false, "PENDING", null, Instant.now().minusSeconds(3600L * 24 * 210), "EMPLOYEE"),
            new PersonRecord("p_10", "Gina Wells", "gina@univerliga.local", "d_2", "t_3", true, "PROVISIONED", "kc_10", Instant.now().minusSeconds(3600L * 24 * 200), "EMPLOYEE"),
            new PersonRecord("p_11", "Helen Moore", "helen@univerliga.local", "d_1", "t_2", true, "PROVISIONED", "kc_11", Instant.now().minusSeconds(3600L * 24 * 180), "EMPLOYEE"),
            new PersonRecord("p_12", "Ilya King", "ilya@univerliga.local", "d_2", "t_3", true, "PROVISIONED", "kc_12", Instant.now().minusSeconds(3600L * 24 * 170), "EMPLOYEE")
        ));
    }

    private void initTasks() {
        tasks.addAll(List.of(
            task("task_1", "Quarter review", "Review Q1 outcomes", "ACTIVE", "2026-01-01", "2026-01-31", "p_manager", "p_employee", List.of("p_employee", "p_4", "p_11")),
            task("task_2", "Onboarding", "New employee onboarding", "ACTIVE", "2026-02-01", "2026-02-28", "p_manager", "p_5", List.of("p_5", "p_6", "p_11")),
            task("task_3", "Culture survey", "Internal feedback cycle", "DRAFT", "2026-02-01", "2026-03-01", "p_admin", "p_7", List.of("p_7", "p_8")),
            task("task_4", "Performance sync", "Team performance check", "ACTIVE", "2026-01-15", "2026-02-15", "p_8", "p_10", List.of("p_10", "p_manager", "p_12")),
            task("task_5", "Risk audit", "Audit monthly risks", "CLOSED", "2025-12-01", "2025-12-31", "p_admin", "p_manager", List.of("p_manager", "p_4")),
            task("task_6", "Hiring review", "Candidate assessment", "ACTIVE", "2026-02-10", "2026-03-10", "p_manager", "p_employee", List.of("p_employee", "p_9")),
            task("task_7", "Vendor check", "Evaluate new vendor", "DRAFT", "2026-03-01", "2026-03-31", "p_admin", "p_8", List.of("p_8", "p_5")),
            task("task_8", "Service KPI", "Monthly KPI sync", "ACTIVE", "2026-01-01", "2026-02-01", "p_8", "p_6", List.of("p_6", "p_7", "p_12")),
            task("task_9", "Quality sprint", "Quality improvements", "ACTIVE", "2026-02-05", "2026-03-05", "p_manager", "p_4", List.of("p_4", "p_employee", "p_10")),
            task("task_10", "Retention", "Retention plan", "CLOSED", "2025-11-01", "2025-11-30", "p_admin", "p_10", List.of("p_10", "p_7"))
        ));
    }

    private void initCategories() {
        categories.addAll(List.of(
            new CategoryRecord("cat_work", "По работе", List.of(
                new CategoryRecord.SubcategoryRecord("sub_comm_good", "Доброжелательная коммуникация", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_expert_high", "Высокая экспертность", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_help_explain", "Помог разобраться", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_deadline_help", "Поддержка в дедлайн", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_comm_bad", "Грубость / некорректность", Polarity.NEGATIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_deadline_fail", "Срыв сроков", Polarity.NEGATIVE, true)
            )),
            new CategoryRecord("cat_collaboration", "Командное взаимодействие", List.of(
                new CategoryRecord.SubcategoryRecord("sub_initiative", "Инициативность", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_ownership", "Ответственность за результат", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_avoid_hard", "Избегает сложных задач", Polarity.NEGATIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_conflict", "Создает конфликтные ситуации", Polarity.NEGATIVE, true)
            )),
            new CategoryRecord("cat_process", "Процессы", List.of(
                new CategoryRecord.SubcategoryRecord("sub_tz_good", "Хорошая постановка задач", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_qa_quality", "Качественная проверка результатов", Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_wrong_advice", "Неверная рекомендация", Polarity.NEGATIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_refuse_help", "Отказ в помощи без причин", Polarity.NEGATIVE, true)
            ))
        ));
    }

    private void initFeedback() {
        List<FeedbackRecord.ContextType> contextTypes = List.of(
            FeedbackRecord.ContextType.TASK,
            FeedbackRecord.ContextType.EPISODE,
            FeedbackRecord.ContextType.HALF_YEAR_REVIEW
        );
        List<String> authors = List.of("p_manager", "p_employee", "p_4", "p_5", "p_6", "p_7", "p_8", "p_10", "p_11", "p_12");
        List<String> targets = List.of("p_employee", "p_4", "p_5", "p_6", "p_7", "p_10", "p_11", "p_12");
        List<String> positiveTags = List.of("sub_comm_good", "sub_expert_high", "sub_help_explain", "sub_deadline_help",
            "sub_initiative", "sub_ownership", "sub_tz_good", "sub_qa_quality");
        List<String> negativeTags = List.of("sub_comm_bad", "sub_deadline_fail", "sub_avoid_hard", "sub_conflict",
            "sub_wrong_advice", "sub_refuse_help");
        for (int i = 1; i <= 55; i++) {
            boolean positive = i % 4 != 0;
            FeedbackRecord.ContextType contextType = contextTypes.get(i % contextTypes.size());
            String contextRef;
            if (contextType == FeedbackRecord.ContextType.TASK) {
                contextRef = "task_" + ((i % 10) + 1);
            } else if (contextType == FeedbackRecord.ContextType.EPISODE) {
                contextRef = "episode_2026_" + ((i % 8) + 1);
            } else {
                contextRef = i % 2 == 0 ? "2026-H1" : "2025-H2";
            }
            String author = authors.get(i % authors.size());
            String target = targets.get((i + 2) % targets.size());
            if (author.equals(target)) {
                target = targets.get((i + 3) % targets.size());
            }
            FeedbackRecord.Sentiment sentiment = positive ? FeedbackRecord.Sentiment.POSITIVE : FeedbackRecord.Sentiment.NEGATIVE;
            List<String> pool = positive ? positiveTags : negativeTags;
            String tag1 = pool.get(i % pool.size());
            String tag2 = pool.get((i + 3) % pool.size());
            List<String> tagIds = tag1.equals(tag2) ? List.of(tag1) : List.of(tag1, tag2);
            int rating = positive ? 4 + (i % 2) : 1 + (i % 2);
            Instant createdAt = Instant.now().minusSeconds(i * 3600L * 8);
            feedback.add(new FeedbackRecord(
                "fb_" + i,
                target,
                author,
                contextType,
                contextRef,
                rating,
                sentiment,
                tagIds,
                "Mock review #" + i,
                createdAt,
                null
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

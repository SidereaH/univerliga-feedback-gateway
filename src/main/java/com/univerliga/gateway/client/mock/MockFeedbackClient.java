package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockFeedbackClient implements FeedbackClient {
    private final MockDataStore dataStore;

    public MockFeedbackClient(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public List<CategoryRecord> categories() {
        return dataStore.categories();
    }

    @Override
    public FeedbackRecord createFeedback(String taskId, String targetPersonId, String authorPersonId, String categoryId, String subcategoryId, int rating, String comment) {
        FeedbackRecord record = new FeedbackRecord(
            "fb_" + UUID.randomUUID().toString().substring(0, 8), taskId, targetPersonId, authorPersonId,
            categoryId, subcategoryId, rating, comment, Instant.now()
        );
        dataStore.feedback().add(record);
        return record;
    }

    @Override
    public List<FeedbackRecord> findByAuthor(String authorPersonId, String taskId) {
        return dataStore.feedback().stream()
            .filter(f -> f.authorPersonId().equals(authorPersonId))
            .filter(f -> taskId == null || taskId.equals(f.taskId()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }

    @Override
    public List<FeedbackRecord> findInbox(String targetPersonId, String taskId) {
        return dataStore.feedback().stream()
            .filter(f -> f.targetPersonId().equals(targetPersonId))
            .filter(f -> taskId == null || taskId.equals(f.taskId()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }

    @Override
    public List<FeedbackRecord> findRaw(String taskId, String targetPersonId, String authorPersonId) {
        return dataStore.feedback().stream()
            .filter(f -> taskId == null || taskId.equals(f.taskId()))
            .filter(f -> targetPersonId == null || targetPersonId.equals(f.targetPersonId()))
            .filter(f -> authorPersonId == null || authorPersonId.equals(f.authorPersonId()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }
}

package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    public FeedbackRecord createReview(FeedbackRecord draft) {
        FeedbackRecord record = new FeedbackRecord(
            "fb_" + UUID.randomUUID().toString().substring(0, 8),
            draft.targetPersonId(),
            draft.authorPersonId(),
            draft.contextType(),
            draft.contextRef(),
            draft.rating(),
            draft.sentiment(),
            draft.tagIds(),
            draft.comment(),
            Instant.now(),
            null
        );
        dataStore.feedback().add(record);
        return record;
    }

    @Override
    public Optional<FeedbackRecord> findById(String reviewId) {
        return dataStore.feedback().stream().filter(f -> f.id().equals(reviewId)).findFirst();
    }

    @Override
    public FeedbackRecord updateReview(String reviewId, Integer rating, FeedbackRecord.Sentiment sentiment, List<String> tagIds, String comment) {
        FeedbackRecord existing = findById(reviewId).orElseThrow();
        FeedbackRecord updated = new FeedbackRecord(
            existing.id(),
            existing.targetPersonId(),
            existing.authorPersonId(),
            existing.contextType(),
            existing.contextRef(),
            rating != null ? rating : existing.rating(),
            sentiment != null ? sentiment : existing.sentiment(),
            tagIds != null ? tagIds : existing.tagIds(),
            comment != null ? comment : existing.comment(),
            existing.createdAt(),
            Instant.now()
        );
        dataStore.feedback().remove(existing);
        dataStore.feedback().add(updated);
        return updated;
    }

    @Override
    public List<FeedbackRecord> findMy(String authorPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        return dataStore.feedback().stream()
            .filter(f -> f.authorPersonId().equals(authorPersonId))
            .filter(f -> contextType == null || contextType == f.contextType())
            .filter(f -> contextRef == null || contextRef.equals(f.contextRef()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }

    @Override
    public List<FeedbackRecord> findInbox(String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        return dataStore.feedback().stream()
            .filter(f -> f.targetPersonId().equals(targetPersonId))
            .filter(f -> contextType == null || contextType == f.contextType())
            .filter(f -> contextRef == null || contextRef.equals(f.contextRef()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }

    @Override
    public List<FeedbackRecord> findRaw(FeedbackRecord.ContextType contextType, String contextRef, String targetPersonId, String authorPersonId) {
        return dataStore.feedback().stream()
            .filter(f -> contextType == null || contextType == f.contextType())
            .filter(f -> contextRef == null || contextRef.equals(f.contextRef()))
            .filter(f -> targetPersonId == null || targetPersonId.equals(f.targetPersonId()))
            .filter(f -> authorPersonId == null || authorPersonId.equals(f.authorPersonId()))
            .sorted(Comparator.comparing(FeedbackRecord::createdAt).reversed())
            .toList();
    }

    @Override
    public Optional<FeedbackRecord> findDuplicate(String authorPersonId, String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        return dataStore.feedback().stream()
            .filter(f -> f.authorPersonId().equals(authorPersonId))
            .filter(f -> f.targetPersonId().equals(targetPersonId))
            .filter(f -> f.contextType() == contextType)
            .filter(f -> f.contextRef().equals(contextRef))
            .findFirst();
    }

    @Override
    public List<FeedbackRecord> findAll() {
        return dataStore.feedback();
    }
}

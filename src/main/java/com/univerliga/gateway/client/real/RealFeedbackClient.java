package com.univerliga.gateway.client.real;

import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealFeedbackClient implements FeedbackClient {
    @Override
    public List<CategoryRecord> categories() {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public FeedbackRecord createFeedback(String taskId, String targetPersonId, String authorPersonId, String categoryId, String subcategoryId, int rating, String comment) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findByAuthor(String authorPersonId, String taskId) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findInbox(String targetPersonId, String taskId) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findRaw(String taskId, String targetPersonId, String authorPersonId) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }
}

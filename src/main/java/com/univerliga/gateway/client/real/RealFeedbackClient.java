package com.univerliga.gateway.client.real;

import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealFeedbackClient implements FeedbackClient {
    private final RestClient restClient;

    public RealFeedbackClient(@Qualifier("feedbackRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<CategoryRecord> categories() {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API via " + restClient);
    }

    @Override
    public FeedbackRecord createReview(FeedbackRecord draft) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public Optional<FeedbackRecord> findById(String reviewId) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public FeedbackRecord updateReview(String reviewId, Integer rating, FeedbackRecord.Sentiment sentiment, List<String> tagIds, String comment) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findMy(String authorPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findInbox(String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findRaw(FeedbackRecord.ContextType contextType, String contextRef, String targetPersonId, String authorPersonId) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public Optional<FeedbackRecord> findDuplicate(String authorPersonId, String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }

    @Override
    public List<FeedbackRecord> findAll() {
        throw new UnsupportedOperationException("TODO: integrate real Feedback HTTP API");
    }
}

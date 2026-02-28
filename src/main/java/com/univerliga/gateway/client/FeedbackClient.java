package com.univerliga.gateway.client;

import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;

import java.util.List;
import java.util.Optional;

public interface FeedbackClient {
    List<CategoryRecord> categories();

    FeedbackRecord createReview(FeedbackRecord draft);

    Optional<FeedbackRecord> findById(String reviewId);

    FeedbackRecord updateReview(String reviewId, Integer rating, FeedbackRecord.Sentiment sentiment, List<String> tagIds, String comment);

    List<FeedbackRecord> findMy(String authorPersonId, FeedbackRecord.ContextType contextType, String contextRef);

    List<FeedbackRecord> findInbox(String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef);

    List<FeedbackRecord> findRaw(FeedbackRecord.ContextType contextType, String contextRef, String targetPersonId, String authorPersonId);

    Optional<FeedbackRecord> findDuplicate(String authorPersonId, String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef);

    List<FeedbackRecord> findAll();
}

package com.univerliga.gateway.client;

import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;

import java.util.List;

public interface FeedbackClient {
    List<CategoryRecord> categories();

    FeedbackRecord createFeedback(String taskId,
                                  String targetPersonId,
                                  String authorPersonId,
                                  String categoryId,
                                  String subcategoryId,
                                  int rating,
                                  String comment);

    List<FeedbackRecord> findByAuthor(String authorPersonId, String taskId);

    List<FeedbackRecord> findInbox(String targetPersonId, String taskId);

    List<FeedbackRecord> findRaw(String taskId, String targetPersonId, String authorPersonId);
}

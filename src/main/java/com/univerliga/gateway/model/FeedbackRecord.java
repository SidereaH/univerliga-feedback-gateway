package com.univerliga.gateway.model;

import java.time.Instant;

public record FeedbackRecord(
    String id,
    String taskId,
    String targetPersonId,
    String authorPersonId,
    String categoryId,
    String subcategoryId,
    int rating,
    String comment,
    Instant createdAt
) {
}

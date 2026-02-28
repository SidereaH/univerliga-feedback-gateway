package com.univerliga.gateway.model;

import java.time.Instant;
import java.util.List;

public record FeedbackRecord(
    String id,
    String targetPersonId,
    String authorPersonId,
    ContextType contextType,
    String contextRef,
    Integer rating,
    Sentiment sentiment,
    List<String> tagIds,
    String comment,
    Instant createdAt,
    Instant updatedAt
) {
    public enum ContextType {
        TASK,
        EPISODE,
        HALF_YEAR_REVIEW
    }

    public enum Sentiment {
        POSITIVE,
        NEGATIVE
    }
}

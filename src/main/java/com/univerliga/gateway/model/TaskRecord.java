package com.univerliga.gateway.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskRecord(
    String id,
    String title,
    String description,
    String status,
    LocalDate periodFrom,
    LocalDate periodTo,
    String ownerId,
    String assigneeId,
    List<String> participantIds,
    Instant createdAt,
    Instant closedAt
) {
}

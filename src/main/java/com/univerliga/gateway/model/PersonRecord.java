package com.univerliga.gateway.model;

import java.time.Instant;

public record PersonRecord(
    String id,
    String displayName,
    String email,
    String departmentId,
    String teamId,
    boolean active,
    String identityStatus,
    String keycloakUserId,
    Instant createdAt,
    String role
) {
}

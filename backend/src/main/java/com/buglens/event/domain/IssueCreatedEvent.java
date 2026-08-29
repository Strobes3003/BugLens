package com.buglens.event.domain;

import java.time.Instant;

public record IssueCreatedEvent(
        Long issueId,
        Long actorId,
        String issueKey,
        String title,
        Instant occurredAt
) implements IssueDomainEvent {

    public static IssueCreatedEvent of(Long issueId, Long actorId, String issueKey, String title) {
        return new IssueCreatedEvent(issueId, actorId, issueKey, title, Instant.now());
    }
}

package com.buglens.event.domain;

import java.time.Instant;
import java.util.List;

public record IssueUpdatedEvent(
        Long issueId,
        Long actorId,
        List<String> changedFields,
        Instant occurredAt
) implements IssueDomainEvent {

    public IssueUpdatedEvent {
        changedFields = List.copyOf(changedFields);
    }

    public static IssueUpdatedEvent of(Long issueId, Long actorId, List<String> changedFields) {
        return new IssueUpdatedEvent(issueId, actorId, changedFields, Instant.now());
    }
}

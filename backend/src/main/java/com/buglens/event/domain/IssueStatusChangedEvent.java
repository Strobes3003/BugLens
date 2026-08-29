package com.buglens.event.domain;

import com.buglens.issue.entity.IssueStatus;

import java.time.Instant;

public record IssueStatusChangedEvent(
        Long issueId,
        Long actorId,
        IssueStatus oldStatus,
        IssueStatus newStatus,
        Instant occurredAt
) implements IssueDomainEvent {

    public static IssueStatusChangedEvent of(
            Long issueId,
            Long actorId,
            IssueStatus oldStatus,
            IssueStatus newStatus
    ) {
        return new IssueStatusChangedEvent(issueId, actorId, oldStatus, newStatus, Instant.now());
    }
}

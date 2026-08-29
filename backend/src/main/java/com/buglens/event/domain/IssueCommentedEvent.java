package com.buglens.event.domain;

import java.time.Instant;

public record IssueCommentedEvent(
        Long issueId,
        Long actorId,
        Long commentId,
        Instant occurredAt
) implements IssueDomainEvent {

    public static IssueCommentedEvent of(Long issueId, Long actorId, Long commentId) {
        return new IssueCommentedEvent(issueId, actorId, commentId, Instant.now());
    }
}

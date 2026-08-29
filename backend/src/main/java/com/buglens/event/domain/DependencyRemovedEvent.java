package com.buglens.event.domain;

import java.time.Instant;

public record DependencyRemovedEvent(
        Long blockerId,
        Long blockedId,
        Long actorId,
        Instant occurredAt
) implements DependencyDomainEvent {

    public static DependencyRemovedEvent of(Long blockerId, Long blockedId, Long actorId) {
        return new DependencyRemovedEvent(blockerId, blockedId, actorId, Instant.now());
    }
}

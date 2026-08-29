package com.buglens.event.domain;

import java.time.Instant;

public record DependencyAddedEvent(
        Long blockerId,
        Long blockedId,
        Long actorId,
        Instant occurredAt
) implements DependencyDomainEvent {

    public static DependencyAddedEvent of(Long blockerId, Long blockedId, Long actorId) {
        return new DependencyAddedEvent(blockerId, blockedId, actorId, Instant.now());
    }
}

package com.buglens.event.domain;

import java.time.Instant;

/**
 * Base type for events raised when the dependency graph changes.
 *
 * <p>Sealed for the same reason as {@link IssueDomainEvent}: the concrete events are records, so
 * a shared base class is not available, and sealing keeps the set closed.
 */
public sealed interface DependencyDomainEvent
        permits DependencyAddedEvent, DependencyRemovedEvent {

    /** The upstream issue — the one whose blast radius just changed. */
    Long blockerId();

    Long blockedId();

    Long actorId();

    Instant occurredAt();
}

package com.buglens.event.domain;

import java.time.Instant;

/**
 * Base type for domain events raised by the issue subsystem.
 *
 * <p>Modelled as a sealed interface rather than a base class because the concrete events are
 * records, and records cannot extend a class. Sealing keeps the set of events closed, so a
 * listener that switches over them can be checked for exhaustiveness at compile time.
 *
 * <p>Events carry identifiers, never entities: they are handled after the publishing transaction
 * has committed, at which point any entity instance captured here would be detached.
 */
public sealed interface IssueDomainEvent
        permits IssueCreatedEvent, IssueUpdatedEvent, IssueStatusChangedEvent, IssueCommentedEvent {

    Long issueId();

    Long actorId();

    Instant occurredAt();
}

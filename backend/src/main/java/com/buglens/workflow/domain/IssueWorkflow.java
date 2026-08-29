package com.buglens.workflow.domain;

import com.buglens.issue.entity.IssueStatus;
import com.buglens.workflow.exception.InvalidStateTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The authoritative issue state machine.
 *
 * <p>The transition matrix is a constant of the domain rather than a collaborator, so it is
 * modelled as an immutable {@link EnumMap} of {@link EnumSet}s and exposed statically. A status
 * absent from the matrix, or a target not listed for the current status, is rejected — including
 * a transition to the status the issue already holds.
 *
 * <pre>
 * OPEN        -> IN_PROGRESS
 * IN_PROGRESS -> IN_REVIEW, OPEN
 * IN_REVIEW   -> RESOLVED, IN_PROGRESS
 * RESOLVED    -> CLOSED, IN_PROGRESS
 * CLOSED      -> OPEN, IN_PROGRESS
 * </pre>
 */
public final class IssueWorkflow {

    private static final Map<IssueStatus, Set<IssueStatus>> TRANSITIONS;

    static {
        EnumMap<IssueStatus, Set<IssueStatus>> transitions = new EnumMap<>(IssueStatus.class);
        transitions.put(IssueStatus.OPEN, EnumSet.of(IssueStatus.IN_PROGRESS));
        transitions.put(IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.IN_REVIEW, IssueStatus.OPEN));
        transitions.put(IssueStatus.IN_REVIEW, EnumSet.of(IssueStatus.RESOLVED, IssueStatus.IN_PROGRESS));
        transitions.put(IssueStatus.RESOLVED, EnumSet.of(IssueStatus.CLOSED, IssueStatus.IN_PROGRESS));
        transitions.put(IssueStatus.CLOSED, EnumSet.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private IssueWorkflow() {
    }

    /**
     * The statuses reachable from {@code current}, in {@link IssueStatus} declaration order.
     */
    public static List<IssueStatus> allowedFrom(IssueStatus current) {
        return List.copyOf(TRANSITIONS.getOrDefault(current, EnumSet.noneOf(IssueStatus.class)));
    }

    public static boolean isAllowed(IssueStatus current, IssueStatus target) {
        return TRANSITIONS.getOrDefault(current, EnumSet.noneOf(IssueStatus.class)).contains(target);
    }

    /**
     * @throws InvalidStateTransitionException when the transition is not part of the matrix
     */
    public static void requireAllowed(IssueStatus current, IssueStatus target) {
        if (!isAllowed(current, target)) {
            throw new InvalidStateTransitionException(current, target, allowedFrom(current));
        }
    }
}

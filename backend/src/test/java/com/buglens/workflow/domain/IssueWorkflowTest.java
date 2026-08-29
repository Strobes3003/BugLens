package com.buglens.workflow.domain;

import com.buglens.issue.entity.IssueStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueWorkflowTest {

    /**
     * Guards against a status being added to {@link IssueStatus} without a matching row in the
     * transition matrix, which would silently create a dead-end state.
     */
    @ParameterizedTest(name = "{0} has at least one outgoing transition")
    @EnumSource(IssueStatus.class)
    void everyStatusHasOutgoingTransitions(IssueStatus status) {
        assertFalse(
                IssueWorkflow.allowedFrom(status).isEmpty(),
                "No transitions defined for " + status + " — update the transition matrix"
        );
    }

    @ParameterizedTest(name = "{0} is not reachable from itself")
    @EnumSource(IssueStatus.class)
    void noStatusTransitionsToItself(IssueStatus status) {
        assertFalse(IssueWorkflow.isAllowed(status, status));
    }

    @ParameterizedTest(name = "every target reachable from {0} is a known status")
    @EnumSource(IssueStatus.class)
    void everyTargetIsReachableAndConsistent(IssueStatus status) {
        for (IssueStatus target : IssueWorkflow.allowedFrom(status)) {
            assertTrue(IssueWorkflow.isAllowed(status, target));
        }
    }

    @Test
    void allowedTransitionsAreImmutable() {
        List<IssueStatus> allowed = IssueWorkflow.allowedFrom(IssueStatus.OPEN);

        assertThrows(UnsupportedOperationException.class, () -> allowed.add(IssueStatus.CLOSED));
    }

    @Test
    void everyStatusExceptOpenIsReachableFromSomewhere() {
        for (IssueStatus target : IssueStatus.values()) {
            boolean reachable = false;
            for (IssueStatus from : IssueStatus.values()) {
                if (IssueWorkflow.isAllowed(from, target)) {
                    reachable = true;
                    break;
                }
            }
            assertTrue(reachable, target + " is unreachable from any other status");
        }
    }
}

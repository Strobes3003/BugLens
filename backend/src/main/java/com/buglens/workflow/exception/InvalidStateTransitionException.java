package com.buglens.workflow.exception;

import com.buglens.issue.entity.IssueStatus;

import java.util.List;

public class InvalidStateTransitionException extends RuntimeException {

    private final transient List<IssueStatus> allowedTransitions;

    public InvalidStateTransitionException(
            IssueStatus current,
            IssueStatus target,
            List<IssueStatus> allowedTransitions
    ) {
        super(buildMessage(current, target, allowedTransitions));
        this.allowedTransitions = List.copyOf(allowedTransitions);
    }

    public List<IssueStatus> getAllowedTransitions() {
        return allowedTransitions;
    }

    private static String buildMessage(
            IssueStatus current,
            IssueStatus target,
            List<IssueStatus> allowedTransitions
    ) {
        if (allowedTransitions.isEmpty()) {
            return "Cannot transition issue from " + current + " to " + target
                    + ": no transitions are allowed from " + current;
        }
        return "Cannot transition issue from " + current + " to " + target
                + ": allowed targets are " + allowedTransitions;
    }
}

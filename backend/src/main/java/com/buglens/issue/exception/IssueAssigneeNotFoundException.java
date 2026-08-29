package com.buglens.issue.exception;

public class IssueAssigneeNotFoundException extends RuntimeException {

    public IssueAssigneeNotFoundException(Long assigneeId) {
        super("Assignee not found: " + assigneeId);
    }
}

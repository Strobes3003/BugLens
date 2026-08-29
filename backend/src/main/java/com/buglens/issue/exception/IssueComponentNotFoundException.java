package com.buglens.issue.exception;

public class IssueComponentNotFoundException extends RuntimeException {

    public IssueComponentNotFoundException(Long componentId) {
        super("Component not found: " + componentId);
    }
}

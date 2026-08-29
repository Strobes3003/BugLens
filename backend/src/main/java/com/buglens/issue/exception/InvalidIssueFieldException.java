package com.buglens.issue.exception;

public class InvalidIssueFieldException extends RuntimeException {

    public InvalidIssueFieldException(String field) {
        super("Issue " + field + " must not be blank");
    }
}

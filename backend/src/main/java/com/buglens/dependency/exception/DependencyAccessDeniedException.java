package com.buglens.dependency.exception;

public class DependencyAccessDeniedException extends RuntimeException {

    public DependencyAccessDeniedException() {
        super("You do not have access to this issue");
    }
}

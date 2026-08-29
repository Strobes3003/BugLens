package com.buglens.component.exception;

public class ComponentAccessDeniedException extends RuntimeException {

    public ComponentAccessDeniedException() {
        super("You do not have permission to perform this component operation");
    }
}

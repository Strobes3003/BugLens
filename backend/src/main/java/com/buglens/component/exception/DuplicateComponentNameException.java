package com.buglens.component.exception;

public class DuplicateComponentNameException extends RuntimeException {

    public DuplicateComponentNameException(String name) {
        super("A component with this name already exists in the project: " + name);
    }
}

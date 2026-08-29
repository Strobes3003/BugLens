package com.buglens.component.exception;

public class InvalidComponentNameException extends RuntimeException {

    public InvalidComponentNameException() {
        super("Component name must not be blank");
    }
}

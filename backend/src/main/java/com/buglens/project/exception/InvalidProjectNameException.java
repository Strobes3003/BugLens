package com.buglens.project.exception;

public class InvalidProjectNameException extends RuntimeException {

    public InvalidProjectNameException() {
        super("Project name cannot be blank");
    }
}

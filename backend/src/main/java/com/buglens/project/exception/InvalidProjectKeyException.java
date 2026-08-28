package com.buglens.project.exception;

public class InvalidProjectKeyException extends RuntimeException {

    public InvalidProjectKeyException(String key) {
        super("Project key must contain 2-10 uppercase letters or numbers: " + key);
    }
}

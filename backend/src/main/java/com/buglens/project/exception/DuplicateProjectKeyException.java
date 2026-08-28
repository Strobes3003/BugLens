package com.buglens.project.exception;

public class DuplicateProjectKeyException extends RuntimeException {

    public DuplicateProjectKeyException(String key) {
        super("Project key is already in use in this workspace: " + key);
    }
}

package com.buglens.project.exception;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        super("You do not have permission to perform this project operation");
    }
}

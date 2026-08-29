package com.buglens.activity.exception;

public class ActivityAccessDeniedException extends RuntimeException {

    public ActivityAccessDeniedException() {
        super("You do not have access to this issue");
    }
}

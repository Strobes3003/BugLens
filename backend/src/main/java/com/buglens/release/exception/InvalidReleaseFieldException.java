package com.buglens.release.exception;

public class InvalidReleaseFieldException extends RuntimeException {

    public InvalidReleaseFieldException(String field) {
        super("Release " + field + " must not be blank");
    }
}

package com.buglens.release.exception;

public class ReleaseNotFoundException extends RuntimeException {

    public ReleaseNotFoundException(Long releaseId) {
        super("Release not found: " + releaseId);
    }
}

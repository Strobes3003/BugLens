package com.buglens.release.exception;

public class ReleaseAccessDeniedException extends RuntimeException {

    public ReleaseAccessDeniedException() {
        super("You do not have permission to perform this release operation");
    }
}

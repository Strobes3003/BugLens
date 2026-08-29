package com.buglens.release.exception;

public class DuplicateReleaseVersionException extends RuntimeException {

    public DuplicateReleaseVersionException(String version) {
        super("A release with this version already exists in the project: " + version);
    }
}

package com.buglens.component.exception;

public class ComponentMemberUserNotFoundException extends RuntimeException {

    public ComponentMemberUserNotFoundException(String email) {
        super("No eligible workspace member exists with email: " + email);
    }
}

package com.buglens.component.exception;

public class ComponentMemberNotFoundException extends RuntimeException {

    public ComponentMemberNotFoundException(Long userId) {
        super("User is not a member of this component: " + userId);
    }
}

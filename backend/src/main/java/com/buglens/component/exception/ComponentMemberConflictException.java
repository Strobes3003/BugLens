package com.buglens.component.exception;

public class ComponentMemberConflictException extends RuntimeException {

    public ComponentMemberConflictException(Long userId) {
        super("User is already a member of this component: " + userId);
    }
}

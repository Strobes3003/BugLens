package com.buglens.component.exception;

public class ComponentNotFoundException extends RuntimeException {

    public ComponentNotFoundException(Long componentId) {
        super("Component not found: " + componentId);
    }
}

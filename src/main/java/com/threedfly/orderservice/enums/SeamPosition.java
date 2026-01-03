package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SeamPosition {
    RANDOM("random"),
    ALIGNED("aligned"),
    NEAREST("nearest"),
    REAR("rear"),
    CUSTOM("custom");

    private final String value;

    SeamPosition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SeamPosition fromValue(String value) {
        for (SeamPosition position : SeamPosition.values()) {
            if (position.value.equalsIgnoreCase(value)) {
                return position;
            }
        }
        throw new IllegalArgumentException("Invalid seam position: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

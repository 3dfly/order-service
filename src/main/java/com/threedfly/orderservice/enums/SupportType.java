package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SupportType {
    TREE_AUTO("tree-auto"),
    TREE("tree"),
    ORGANIC("organic"),
    NORMAL("normal");

    private final String value;

    SupportType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SupportType fromValue(String value) {
        for (SupportType type : SupportType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid support type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

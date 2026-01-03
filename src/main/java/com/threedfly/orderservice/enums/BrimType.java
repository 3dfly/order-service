package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BrimType {
    AUTO("auto"),
    NONE("none"),
    CUSTOM("custom");

    private final String value;

    BrimType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BrimType fromValue(String value) {
        for (BrimType type : BrimType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid brim type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BrimType {
    AUTO("auto"),
    NONE("none"),
    INNER_BRIM_ONLY("inner_brim_only"),
    OUTER_AND_INNER_BRIM("outer_and_inner_brim"),
    OUTER_BRIM_ONLY("outer_brim_only"),
    PAINTED("painted");

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

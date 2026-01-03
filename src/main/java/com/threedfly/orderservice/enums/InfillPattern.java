package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InfillPattern {
    GRID("grid"),
    GYROID("gyroid"),
    HONEYCOMB("honeycomb"),
    LINE("line"),
    RECTILINEAR("rectilinear"),
    CUBIC("cubic"),
    TRIANGLES("triangles"),
    CONCENTRIC("concentric"),
    HILBERTCURVE("hilbertcurve"),
    ARCHIMEDEANCHORDS("archimedeanchords"),
    OCTAGRAMSPIRAL("octagramspiral"),
    ADAPTIVECUBIC("adaptivecubic"),
    SUPPORTCUBIC("supportcubic"),
    LIGHTNING("lightning"),
    CROSSHATCH("crosshatch"),
    CROSS3D("cross3d"),
    HONEYCOMB3D("honeycomb3d");

    private final String value;

    InfillPattern(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InfillPattern fromValue(String value) {
        for (InfillPattern pattern : InfillPattern.values()) {
            if (pattern.value.equalsIgnoreCase(value)) {
                return pattern;
            }
        }
        throw new IllegalArgumentException("Invalid infill pattern: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

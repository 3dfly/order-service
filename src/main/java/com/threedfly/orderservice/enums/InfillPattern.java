package com.threedfly.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InfillPattern {
    ADAPTIVECUBIC("adaptivecubic"),
    ALIGNEDRECTILINEAR("alignedrectilinear"),
    ARCHIMEDEANCHORDS("archimedeanchords"),
    CONCENTRIC("concentric"),
    CROSS3D("cross3d"),
    CROSSHATCH("crosshatch"),
    CROSSZAG("crosszag"),
    CUBIC("cubic"),
    GRID("grid"),
    GYROID("gyroid"),
    HILBERTCURVE("hilbertcurve"),
    HONEYCOMB("honeycomb"),
    HONEYCOMB3D("honeycomb3d"),
    LIGHTNING("lightning"),
    LINE("line"),
    LOCKEDZAG("lockedzag"),
    OCTAGRAMSPIRAL("octagramspiral"),
    RECTILINEAR("rectilinear"),
    SUPPORTCUBIC("supportcubic"),
    TRIANGLES("triangles"),
    TRIHEXAGON("trihexagon"),
    ZIGZAG("zigzag");

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

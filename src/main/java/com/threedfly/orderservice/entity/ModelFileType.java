package com.threedfly.orderservice.entity;

import com.threedfly.orderservice.exception.InvalidFileTypeException;

public enum ModelFileType {
    STL(".stl"),
    OBJ(".obj"),
    THREE_MF(".3mf");

    private final String extension;

    ModelFileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static ModelFileType fromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileTypeException("Filename cannot be null or empty");
        }

        String lower = filename.toLowerCase();
        for (ModelFileType type : values()) {
            if (lower.endsWith(type.extension)) {
                return type;
            }
        }

        throw new InvalidFileTypeException(
            "Unsupported file type. Supported formats: STL, OBJ, 3MF");
    }
}

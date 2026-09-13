package com.example.schemadiff.core.model;

import java.util.Locale;

public record ColumnDefinition(
        String name,
        String type,
        Integer size,
        Integer scale,
        boolean nullable,
        String defaultValue,
        boolean autoIncrement
) {
    public ColumnDefinition {
        name = normalize(name);
        type = normalize(type);
        defaultValue = defaultValue == null ? null : defaultValue.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

package com.example.schemadiff.core.diff;

public record SchemaDifference(
        String category,
        String objectName,
        DifferenceType type,
        String sourceDefinition,
        String targetDefinition
) {
}

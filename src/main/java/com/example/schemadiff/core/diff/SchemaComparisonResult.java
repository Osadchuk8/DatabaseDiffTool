package com.example.schemadiff.core.diff;

import java.util.List;

public record SchemaComparisonResult(String sourceName, String targetName, List<SchemaDifference> differences) {
    public SchemaComparisonResult {
        differences = List.copyOf(differences);
    }

    public boolean matches() {
        return differences.isEmpty();
    }
}

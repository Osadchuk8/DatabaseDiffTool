package com.example.schemadiff.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record SchemaSnapshot(String name, Map<String, TableDefinition> tables) {
    public SchemaSnapshot {
        tables = Map.copyOf(new LinkedHashMap<>(tables));
    }
}

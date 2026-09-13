package com.example.schemadiff.core.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TableDefinition(
        String name,
        Map<String, ColumnDefinition> columns,
        List<String> primaryKeyColumns,
        Map<String, ForeignKeyDefinition> foreignKeys,
        Map<String, IndexDefinition> indexes
) {
    public TableDefinition {
        columns = Map.copyOf(new LinkedHashMap<>(columns));
        primaryKeyColumns = List.copyOf(primaryKeyColumns);
        foreignKeys = Map.copyOf(new LinkedHashMap<>(foreignKeys));
        indexes = Map.copyOf(new LinkedHashMap<>(indexes));
    }
}

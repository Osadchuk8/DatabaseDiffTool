package com.example.schemadiff.core.model;

import java.util.List;

public record ForeignKeyDefinition(
        String name,
        List<String> columns,
        String referencedSchema,
        String referencedTable,
        List<String> referencedColumns,
        short updateRule,
        short deleteRule
) {
    public ForeignKeyDefinition {
        columns = List.copyOf(columns);
        referencedColumns = List.copyOf(referencedColumns);
    }
}

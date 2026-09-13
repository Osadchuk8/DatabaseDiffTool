package com.example.schemadiff.core.model;

import java.util.List;

public record IndexDefinition(String name, boolean unique, List<String> columns) {
    public IndexDefinition {
        columns = List.copyOf(columns);
    }
}

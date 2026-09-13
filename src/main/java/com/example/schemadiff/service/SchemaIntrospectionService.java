package com.example.schemadiff.service;

import com.example.schemadiff.configuration.DatabaseConnectionSettings;
import com.example.schemadiff.core.model.SchemaSnapshot;
import com.example.schemadiff.infrastructure.jdbc.JdbcSchemaReader;

import java.sql.SQLException;

public final class SchemaIntrospectionService {
    private final JdbcSchemaReader jdbcSchemaReader;

    public SchemaIntrospectionService(JdbcSchemaReader jdbcSchemaReader) {
        this.jdbcSchemaReader = jdbcSchemaReader;
    }

    public SchemaSnapshot read(DatabaseConnectionSettings settings) throws SQLException {
        return jdbcSchemaReader.read(settings);
    }
}

package com.example.schemadiff.infrastructure.jdbc;

import com.example.schemadiff.configuration.DatabaseConnectionSettings;
import com.example.schemadiff.core.model.*;

import java.sql.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class JdbcSchemaReader {
    public SchemaSnapshot read(DatabaseConnectionSettings settings) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.url(), settings.user(), settings.password())) {
            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, TableDefinition> tables = new TreeMap<>();

            try (ResultSet result = metadata.getTables(connection.getCatalog(), settings.schema(), "%", new String[]{"TABLE"})) {
                while (result.next()) {
                    String tableName = normalize(result.getString("TABLE_NAME"));
                    tables.put(tableName, readTable(metadata, connection.getCatalog(), settings.schema(), tableName));
                }
            }
            return new SchemaSnapshot(settings.schema(), tables);
        }
    }

    private TableDefinition readTable(DatabaseMetaData metadata, String catalog, String schema, String tableName) throws SQLException {
        Map<String, ColumnDefinition> columns = readColumns(metadata, catalog, schema, tableName);
        List<String> primaryKey = readPrimaryKey(metadata, catalog, schema, tableName);
        Map<String, ForeignKeyDefinition> foreignKeys = readForeignKeys(metadata, catalog, schema, tableName);
        Map<String, IndexDefinition> indexes = readIndexes(metadata, catalog, schema, tableName);
        return new TableDefinition(tableName, columns, primaryKey, foreignKeys, indexes);
    }

    private Map<String, ColumnDefinition> readColumns(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Map<String, ColumnDefinition> columns = new TreeMap<>();
        try (ResultSet result = metadata.getColumns(catalog, schema, table, "%")) {
            while (result.next()) {
                String name = normalize(result.getString("COLUMN_NAME"));
                columns.put(name, new ColumnDefinition(
                        name,
                        result.getString("TYPE_NAME"),
                        nullableInteger(result, "COLUMN_SIZE"),
                        nullableInteger(result, "DECIMAL_DIGITS"),
                        result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                        result.getString("COLUMN_DEF"),
                        "YES".equalsIgnoreCase(result.getString("IS_AUTOINCREMENT"))
                ));
            }
        }
        return columns;
    }

    private List<String> readPrimaryKey(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Map<Short, String> columns = new TreeMap<>();
        try (ResultSet result = metadata.getPrimaryKeys(catalog, schema, table)) {
            while (result.next()) {
                columns.put(result.getShort("KEY_SEQ"), normalize(result.getString("COLUMN_NAME")));
            }
        }
        return List.copyOf(columns.values());
    }

    private Map<String, ForeignKeyDefinition> readForeignKeys(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Map<String, ForeignKeyBuilder> builders = new TreeMap<>();
        try (ResultSet result = metadata.getImportedKeys(catalog, schema, table)) {
            while (result.next()) {
                String fallbackName = "fk_" + table + "_" + normalize(result.getString("FKCOLUMN_NAME"));
                String name = normalizeOrDefault(result.getString("FK_NAME"), fallbackName);
                String referencedSchema = normalize(result.getString("PKTABLE_SCHEM"));
                String referencedTable = normalize(result.getString("PKTABLE_NAME"));
                short updateRule = result.getShort("UPDATE_RULE");
                short deleteRule = result.getShort("DELETE_RULE");
                ForeignKeyBuilder builder = builders.computeIfAbsent(name, ignored -> new ForeignKeyBuilder(
                        name, referencedSchema, referencedTable, updateRule, deleteRule
                ));
                builder.add(result.getShort("KEY_SEQ"), normalize(result.getString("FKCOLUMN_NAME")), normalize(result.getString("PKCOLUMN_NAME")));
            }
        }
        Map<String, ForeignKeyDefinition> foreignKeys = new TreeMap<>();
        builders.forEach((name, builder) -> {
            ForeignKeyDefinition foreignKey = builder.build();
            foreignKeys.put(foreignKeySignature(foreignKey), withoutName(foreignKey));
        });
        return foreignKeys;
    }

    private Map<String, IndexDefinition> readIndexes(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Map<String, IndexBuilder> builders = new TreeMap<>();
        try (ResultSet result = metadata.getIndexInfo(catalog, schema, table, false, false)) {
            while (result.next()) {
                if (result.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic || result.getString("INDEX_NAME") == null) {
                    continue;
                }
                String name = normalize(result.getString("INDEX_NAME"));
                boolean unique = !result.getBoolean("NON_UNIQUE");
                IndexBuilder builder = builders.computeIfAbsent(name,
                        ignored -> new IndexBuilder(name, unique));
                String column = result.getString("COLUMN_NAME");
                if (column != null) {
                    builder.add(result.getShort("ORDINAL_POSITION"), normalize(column));
                }
            }
        }
        Map<String, IndexDefinition> indexes = new TreeMap<>();
        builders.forEach((name, builder) -> {
            IndexDefinition index = builder.build();
            indexes.put(indexSignature(index), new IndexDefinition("", index.unique(), index.columns()));
        });
        return indexes;
    }

    private Integer nullableInteger(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private String foreignKeySignature(ForeignKeyDefinition foreignKey) {
        return String.join(",", foreignKey.columns()) + "->" + foreignKey.referencedSchema() + "."
                + foreignKey.referencedTable() + "(" + String.join(",", foreignKey.referencedColumns()) + ")";
    }

    private ForeignKeyDefinition withoutName(ForeignKeyDefinition foreignKey) {
        return new ForeignKeyDefinition("", foreignKey.columns(), foreignKey.referencedSchema(), foreignKey.referencedTable(),
                foreignKey.referencedColumns(), foreignKey.updateRule(), foreignKey.deleteRule());
    }

    private String indexSignature(IndexDefinition index) {
        return (index.unique() ? "unique" : "nonunique") + "(" + String.join(",", index.columns()) + ")";
    }

    private static final class ForeignKeyBuilder {
        private final String name;
        private final String referencedSchema;
        private final String referencedTable;
        private final short updateRule;
        private final short deleteRule;
        private final Map<Short, String> columns = new TreeMap<>();
        private final Map<Short, String> referencedColumns = new TreeMap<>();

        private ForeignKeyBuilder(String name, String referencedSchema, String referencedTable, short updateRule, short deleteRule) {
            this.name = name;
            this.referencedSchema = referencedSchema;
            this.referencedTable = referencedTable;
            this.updateRule = updateRule;
            this.deleteRule = deleteRule;
        }

        private void add(short sequence, String column, String referencedColumn) {
            columns.put(sequence, column);
            referencedColumns.put(sequence, referencedColumn);
        }

        private ForeignKeyDefinition build() {
            return new ForeignKeyDefinition(name, List.copyOf(columns.values()), referencedSchema, referencedTable,
                    List.copyOf(referencedColumns.values()), updateRule, deleteRule);
        }
    }

    private static final class IndexBuilder {
        private final String name;
        private final boolean unique;
        private final Map<Short, String> columns = new TreeMap<>();

        private IndexBuilder(String name, boolean unique) {
            this.name = name;
            this.unique = unique;
        }

        private void add(short sequence, String column) {
            columns.put(sequence, column);
        }

        private IndexDefinition build() {
            return new IndexDefinition(name, unique, List.copyOf(columns.values()));
        }
    }
}

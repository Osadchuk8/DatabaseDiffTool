package com.example.schemadiff.infrastructure.file;

import com.example.schemadiff.core.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the PostgreSQL CREATE TABLE and CREATE INDEX subset used by this proof of concept. */
public final class PostgresDdlSchemaReader {
    private static final Pattern TABLE_START = Pattern.compile("(?is)CREATE\\s+TABLE\\s+([\\w\\\"]+)\\.([\\w\\\"]+)\\s*\\(");
    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "(?is)CONSTRAINT\\s+([\\w\\\"]+)\\s+FOREIGN\\s+KEY\\s*\\(([^)]*)\\)\\s*REFERENCES\\s+([\\w\\\"]+)\\.([\\w\\\"]+)\\s*\\(([^)]*)\\)");
    private static final Pattern INLINE_REFERENCE = Pattern.compile(
            "(?is)REFERENCES\\s+([\\w\\\"]+)\\.([\\w\\\"]+)\\s*\\(([^)]*)\\)");
    private static final Pattern INDEX = Pattern.compile(
            "(?is)CREATE\\s+(UNIQUE\\s+)?INDEX\\s+([\\w\\\"]+)\\s+ON\\s+([\\w\\\"]+)\\.([\\w\\\"]+)\\s*\\(([^)]*)\\)");

    public SchemaSnapshot read(Path path, String requestedSchema) throws IOException {
        String sql = Files.readString(path);
        String schema = normalize(requestedSchema);
        Map<String, TableDefinition> tables = new TreeMap<>();

        Matcher matcher = TABLE_START.matcher(sql);
        while (matcher.find()) {
            if (!normalize(matcher.group(1)).equals(schema)) {
                continue;
            }
            int openParenthesis = matcher.end() - 1;
            int closeParenthesis = findClosingParenthesis(sql, openParenthesis);
            if (closeParenthesis < 0) {
                throw new IllegalArgumentException("Unclosed CREATE TABLE statement for " + matcher.group(2));
            }
            String tableName = normalize(matcher.group(2));
            String tableBody = sql.substring(openParenthesis + 1, closeParenthesis);
            tables.put(tableName, parseTable(tableName, tableBody));
        }

        addIndexes(sql, schema, tables);
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("No CREATE TABLE statements found for schema '" + requestedSchema + "' in " + path);
        }
        return new SchemaSnapshot(requestedSchema, tables);
    }

    private TableDefinition parseTable(String tableName, String body) {
        Map<String, ColumnDefinition> columns = new TreeMap<>();
        List<String> primaryKey = new ArrayList<>();
        Map<String, ForeignKeyDefinition> foreignKeys = new TreeMap<>();

        for (String item : splitTopLevel(body)) {
            String trimmed = item.trim();
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (upper.startsWith("CONSTRAINT") && upper.contains("FOREIGN KEY")) {
                ForeignKeyDefinition foreignKey = parseForeignKey(trimmed);
                foreignKeys.put(foreignKeySignature(foreignKey), withoutName(foreignKey));
            } else if (upper.startsWith("CONSTRAINT") && upper.contains("PRIMARY KEY")) {
                primaryKey.addAll(parenthesizedValues(trimmed));
            } else if (!upper.startsWith("CONSTRAINT") && !upper.startsWith("PRIMARY KEY")
                    && !upper.startsWith("FOREIGN KEY") && !upper.startsWith("UNIQUE") && !upper.startsWith("CHECK")) {
                ColumnDefinition column = parseColumn(trimmed);
                columns.put(column.name(), column);
                if (upper.contains("PRIMARY KEY")) {
                    primaryKey.add(column.name());
                }
                Matcher inlineReference = INLINE_REFERENCE.matcher(trimmed);
                if (inlineReference.find()) {
                    String name = "fk_" + tableName + "_" + column.name();
                    ForeignKeyDefinition foreignKey = new ForeignKeyDefinition(name, List.of(column.name()),
                            // normalize(inlineReference.group(1)), schema name
                            normalize(inlineReference.group(2)), normalizeList(inlineReference.group(3)), (short) 3, (short) 3);
                    foreignKeys.put(foreignKeySignature(foreignKey), withoutName(foreignKey));
                }
            }
        }
        return new TableDefinition(tableName, columns, primaryKey, foreignKeys, new TreeMap<>());
    }

    private ColumnDefinition parseColumn(String definition) {
        Matcher matcher = Pattern.compile("(?is)^([\\w\\\"]+)\\s+(.+)$").matcher(definition);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Could not parse column definition: " + definition);
        }
        String name = normalize(matcher.group(1));
        String remainder = matcher.group(2).trim();
        String type = extractType(remainder);
        Integer size = extractNumber(type, 1);
        Integer scale = extractNumber(type, 2);
        type = type.replaceAll("(?is)\\(.*\\)", "").replaceFirst("(?is)^\\w+\\.", "").trim();
        boolean nullable = !remainder.toUpperCase(Locale.ROOT).contains("NOT NULL")
                && !remainder.toUpperCase(Locale.ROOT).contains("PRIMARY KEY");
        String defaultValue = extractDefault(remainder);
        boolean generated = remainder.toUpperCase(Locale.ROOT).contains("GENERATED")
                && remainder.toUpperCase(Locale.ROOT).contains("IDENTITY");
        return new ColumnDefinition(name, type, size, scale, nullable, defaultValue, generated);
    }

    private String extractType(String remainder) {
        Matcher marker = Pattern.compile("(?is)\\s+(?:NOT\\s+NULL|NULL|DEFAULT|REFERENCES|CONSTRAINT|PRIMARY\\s+KEY|UNIQUE|CHECK|GENERATED)\\b")
                .matcher(remainder);
        return marker.find() ? remainder.substring(0, marker.start()).trim() : remainder.trim();
    }

    private String extractDefault(String remainder) {
        Matcher matcher = Pattern.compile("(?is)\\bDEFAULT\\s+(.+?)(?=\\s+(?:NOT\\s+NULL|NULL|REFERENCES|CONSTRAINT|PRIMARY|UNIQUE|CHECK|GENERATED)\\b|$)")
                .matcher(remainder);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Integer extractNumber(String type, int position) {
        Matcher matcher = Pattern.compile("\\((\\d+)(?:\\s*,\\s*(\\d+))?\\)").matcher(type);
        if (!matcher.find() || matcher.group(position) == null) {
            return null;
        }
        return Integer.valueOf(matcher.group(position));
    }

    private ForeignKeyDefinition parseForeignKey(String definition) {
        Matcher matcher = FOREIGN_KEY.matcher(definition);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not parse foreign key definition: " + definition);
        }
        return new ForeignKeyDefinition(normalize(matcher.group(1)), normalizeList(matcher.group(2)),
                // normalize(matcher.group(3)), -- schema name
                normalize(matcher.group(4)), normalizeList(matcher.group(5)), (short) 3, (short) 3);
    }

    private void addIndexes(String sql, String schema, Map<String, TableDefinition> tables) {
        Map<String, Map<String, IndexDefinition>> indexesByTable = new TreeMap<>();
        Matcher matcher = INDEX.matcher(sql);
        while (matcher.find()) {
            if (!normalize(matcher.group(3)).equals(schema)) {
                continue;
            }
            String table = normalize(matcher.group(4));
            IndexDefinition index = new IndexDefinition(normalize(matcher.group(2)), matcher.group(1) != null, normalizeList(matcher.group(5)));
            indexesByTable.computeIfAbsent(table, ignored -> new TreeMap<>()).put(indexSignature(index),
                    new IndexDefinition("", index.unique(), index.columns()));
        }
        indexesByTable.forEach((tableName, indexes) -> {
            TableDefinition table = tables.get(tableName);
            if (table != null) {
                tables.put(tableName, new TableDefinition(table.name(), table.columns(), table.primaryKeyColumns(), table.foreignKeys(), indexes));
            }
        });
    }

    private List<String> splitTopLevel(String value) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\'') quoted = !quoted;
            if (!quoted && current == '(') depth++;
            if (!quoted && current == ')') depth--;
            if (!quoted && depth == 0 && current == ',') {
                items.add(value.substring(start, index));
                start = index + 1;
            }
        }
        items.add(value.substring(start));
        return items;
    }

    private int findClosingParenthesis(String value, int openingPosition) {
        int depth = 0;
        for (int index = openingPosition; index < value.length(); index++) {
            if (value.charAt(index) == '(') depth++;
            if (value.charAt(index) == ')' && --depth == 0) return index;
        }
        return -1;
    }

    private List<String> parenthesizedValues(String value) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        return open >= 0 && close > open ? normalizeList(value.substring(open + 1, close)) : List.of();
    }

    private List<String> normalizeList(String values) {
        return List.of(values.split(",")).stream().map(this::normalize).toList();
    }

    private String normalize(String value) {
        return value.replace("\"", "").trim().toLowerCase(Locale.ROOT);
    }

    private String foreignKeySignature(ForeignKeyDefinition foreignKey) {
        return String.join(",", foreignKey.columns()) + "->"
                + foreignKey.referencedTable() + "(" + String.join(",", foreignKey.referencedColumns()) + ")";
    }

    private ForeignKeyDefinition withoutName(ForeignKeyDefinition foreignKey) {
        return new ForeignKeyDefinition("", foreignKey.columns(), foreignKey.referencedTable(),
                foreignKey.referencedColumns(), foreignKey.updateRule(), foreignKey.deleteRule());
    }

    private String indexSignature(IndexDefinition index) {
        return (index.unique() ? "unique" : "nonunique") + "(" + String.join(",", index.columns()) + ")";
    }
}

package com.example.schemadiff.service;

import com.example.schemadiff.core.diff.DifferenceType;
import com.example.schemadiff.core.diff.SchemaComparisonResult;
import com.example.schemadiff.core.diff.SchemaDifference;
import com.example.schemadiff.core.model.ColumnDefinition;
import com.example.schemadiff.core.model.SchemaSnapshot;
import com.example.schemadiff.core.model.TableDefinition;

import java.util.*;

public final class SchemaComparisonService {
    public SchemaComparisonResult compare(SchemaSnapshot source, SchemaSnapshot target) {
        var differences = new ArrayList<SchemaDifference>();
        var tableNames = union(source.tables().keySet(), target.tables().keySet());

        for (String tableName : tableNames) {
            TableDefinition sourceTable = source.tables().get(tableName);
            TableDefinition targetTable = target.tables().get(tableName);
            String objectName = tableName;

            if (sourceTable == null) {
                differences.add(unexpected("table", objectName, describe(targetTable)));
            } else if (targetTable == null) {
                differences.add(missing("table", objectName, describe(sourceTable)));
            } else {
                compareTable(tableName, sourceTable, targetTable, differences);
            }
        }
        return new SchemaComparisonResult(source.name(), target.name(), differences);
    }

    private void compareTable(String tableName, TableDefinition source, TableDefinition target,
                              Collection<SchemaDifference> differences) {
        for (String columnName : union(source.columns().keySet(), target.columns().keySet())) {
            ColumnDefinition sourceColumn = source.columns().get(columnName);
            ColumnDefinition targetColumn = target.columns().get(columnName);
            String objectName = tableName + "." + columnName;
            if (sourceColumn == null) {
                differences.add(unexpected("column", objectName, describe(targetColumn)));
            } else if (targetColumn == null) {
                differences.add(missing("column", objectName, describe(sourceColumn)));
            } else if (!sourceColumn.equals(targetColumn)) {
                differences.add(changed("column", objectName, describe(sourceColumn), describe(targetColumn)));
            }
        }

        if (!source.primaryKeyColumns().equals(target.primaryKeyColumns())) {
            differences.add(changed("primary key", tableName, source.primaryKeyColumns().toString(), target.primaryKeyColumns().toString()));
        }
        compareNamedObjects("foreign key", tableName, source.foreignKeys(), target.foreignKeys(), differences);
        compareNamedObjects("index", tableName, source.indexes(), target.indexes(), differences);
    }

    private void compareNamedObjects(String category, String tableName, Map<String, ?> source,
                                     Map<String, ?> target, Collection<SchemaDifference> differences) {
        for (String name : union(source.keySet(), target.keySet())) {
            Object sourceValue = source.get(name);
            Object targetValue = target.get(name);
            String objectName = tableName + "." + name;
            if (sourceValue == null) {
                differences.add(unexpected(category, objectName, targetValue.toString()));
            } else if (targetValue == null) {
                differences.add(missing(category, objectName, sourceValue.toString()));
            } else if (!sourceValue.equals(targetValue)) {
                differences.add(changed(category, objectName, sourceValue.toString(), targetValue.toString()));
            }
        }
    }

    private Set<String> union(Collection<String> first, Collection<String> second) {
        var result = new TreeSet<String>();
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    private SchemaDifference missing(String category, String name, String sourceDefinition) {
        return new SchemaDifference(category, name, DifferenceType.MISSING_FROM_TARGET, sourceDefinition, "—");
    }

    private SchemaDifference unexpected(String category, String name, String targetDefinition) {
        return new SchemaDifference(category, name, DifferenceType.UNEXPECTED_IN_TARGET, "—", targetDefinition);
    }

    private SchemaDifference changed(String category, String name, String sourceDefinition, String targetDefinition) {
        return new SchemaDifference(category, name, DifferenceType.CHANGED, sourceDefinition, targetDefinition);
    }

    private String describe(TableDefinition table) {
        return table.columns().size() + " columns";
    }

    private String describe(ColumnDefinition column) {
        String precision = column.size() == null ? "" : "(" + column.size() + (column.scale() == null ? "" : "," + column.scale()) + ")";
        return column.type() + precision + (column.nullable() ? " nullable" : " not null")
                + (column.defaultValue() == null ? "" : " default=" + column.defaultValue())
                + (column.autoIncrement() ? " identity" : "");
    }
}

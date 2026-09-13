package com.example.schemadiff.service;

import com.example.schemadiff.core.diff.SchemaComparisonResult;
import com.example.schemadiff.core.diff.SchemaDifference;
import com.example.schemadiff.infrastructure.report.MarkdownReportWriter;

import java.io.IOException;
import java.nio.file.Path;

public final class ReportGenerationService {
    private final MarkdownReportWriter writer;

    public ReportGenerationService(MarkdownReportWriter writer) {
        this.writer = writer;
    }

    public void write(SchemaComparisonResult result, Path path) throws IOException {
        writer.write(result, path);
    }

    public void write(String result, String reportTitle, Path path) throws IOException {
        writer.write(result, reportTitle, path);
    }

    public String convertToString(SchemaComparisonResult result) {

        StringBuilder report = new StringBuilder("Schema Comparison Report\n\n")
                .append("**Source:** `").append(result.sourceName()).append("`  \n")
                .append("**Target:** `").append(result.targetName()).append("`\n\n");

        report.append("Found **").append(result.differences().size()).append("** structural difference(s).\n")
                .append("| Type | Category | Object | Source definition | Target definition |\n")
                .append("| --- | --- | --- | --- | --- |\n");
        for (SchemaDifference difference : result.differences()) {
            report.append("| ").append(difference.type()).append(" | ")
                    .append((difference.category())).append(" | `")
                    .append((difference.objectName())).append("` | ")
                    .append((difference.sourceDefinition())).append(" | ")
                    .append((difference.targetDefinition())).append(" |\n");
        }
        return report.toString();
    }

}

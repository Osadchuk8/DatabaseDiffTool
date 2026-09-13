package com.example.schemadiff.infrastructure.report;

import com.example.schemadiff.core.diff.SchemaComparisonResult;
import com.example.schemadiff.core.diff.SchemaDifference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MarkdownReportWriter {
    public void write(SchemaComparisonResult result, Path outputPath) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder report = new StringBuilder("# Schema Comparison Report\n\n")
                .append("**Source:** `").append(result.sourceName()).append("`  \n")
                .append("**Target:** `").append(result.targetName()).append("`\n\n");

        if (result.matches()) {
            report.append("## Result\n\nThe schemas match. No structural differences were found.\n");
        } else {
            report.append("## Result\n\nFound **").append(result.differences().size()).append("** structural difference(s).\n\n")
                    .append("| Type | Category | Object | Source definition | Target definition |\n")
                    .append("| --- | --- | --- | --- | --- |\n");
            for (SchemaDifference difference : result.differences()) {
                report.append("| ").append(difference.type()).append(" | ")
                        .append(escape(difference.category())).append(" | `")
                        .append(escape(difference.objectName())).append("` | ")
                        .append(escape(difference.sourceDefinition())).append(" | ")
                        .append(escape(difference.targetDefinition())).append(" |\n");
            }
        }
        Files.writeString(outputPath, report);
    }

    private String escape(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }
}

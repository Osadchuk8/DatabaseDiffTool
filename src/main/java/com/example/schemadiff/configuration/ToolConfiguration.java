package com.example.schemadiff.configuration;

import java.nio.file.Path;

public record ToolConfiguration(
        DatabaseConnectionSettings sourceDatabase,
        Path sourceFile,
        String sourceFileSchema,
        DatabaseConnectionSettings targetDatabase,
        Path reportPath,
        String aiApiKey,
        Path aiReportPath
) {
}

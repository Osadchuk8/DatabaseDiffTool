package com.example.schemadiff.service;

import com.example.schemadiff.core.diff.SchemaComparisonResult;
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
}

package com.example.schemadiff.app;

import com.example.schemadiff.configuration.ConnectionPropertiesLoader;
import com.example.schemadiff.configuration.ToolConfiguration;
import com.example.schemadiff.core.diff.SchemaComparisonResult;
import com.example.schemadiff.core.model.SchemaSnapshot;
import com.example.schemadiff.infrastructure.file.PostgresDdlSchemaReader;
import com.example.schemadiff.infrastructure.jdbc.JdbcSchemaReader;
import com.example.schemadiff.infrastructure.report.MarkdownReportWriter;
import com.example.schemadiff.service.AiApiRequestService;
import com.example.schemadiff.service.ReportGenerationService;
import com.example.schemadiff.service.SchemaComparisonService;
import com.example.schemadiff.service.SchemaIntrospectionService;

import java.nio.file.Path;

public final class SchemaDiffApplication {

    private final static String AI_REPORT_TITLE = "AI-suggestion report";
    private final static String AI_API_REQUEST = "Suggest what is important in the provided database schema comparison";

    private SchemaDiffApplication() {
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = Arguments.parse(args);
            if (arguments.help()) {
                printUsage();
                return;
            }

            ToolConfiguration configuration = new ConnectionPropertiesLoader().load(arguments.configurationPath());
            SchemaIntrospectionService introspection = new SchemaIntrospectionService(new JdbcSchemaReader());
            SchemaSnapshot target = introspection.read(configuration.targetDatabase());
            SchemaSnapshot source = switch (arguments.sourceType()) {
                case DATABASE -> introspection.read(configuration.sourceDatabase());
                case FILE ->
                        new PostgresDdlSchemaReader().read(configuration.sourceFile(), configuration.sourceFileSchema());
            };
            SchemaComparisonService comparisonService = new SchemaComparisonService();
            SchemaComparisonResult comparison = comparisonService.compare(source, target);
            ReportGenerationService reportService = new ReportGenerationService(new MarkdownReportWriter());

            printHeader();
            System.out.printf("\nComparing: %s to %s", comparison.sourceName(), comparison.targetName());

            reportService.write(comparison, configuration.reportPath());
            printComparingMsg(comparison, configuration);

            if (!comparison.matches() && configuration.useAi()) {
                System.out.println("Requesting AI-api suggestions ...");
                AiApiRequestService aiApiRequestService = new AiApiRequestService(configuration.aiApiKey());
                String stringComparisonResult = reportService.convertToString(comparison);
                String aiFindings = aiApiRequestService.generate(AI_API_REQUEST + "\n" + stringComparisonResult);
                reportService.write(aiFindings, AI_REPORT_TITLE, configuration.aiReportPath());
            }

        } catch (Exception exception) {
            System.err.println("Schema comparison failed: " + exception.getMessage());
            System.exit(1);
        }
        printFooter();
    }

    private static void printComparingMsg(SchemaComparisonResult comparison, ToolConfiguration configuration) {
        System.out.println("\n");
        System.out.printf("Compared %s to %s: %d difference(s)", comparison.sourceName(), comparison.targetName(), comparison.differences().size());
        System.out.println("\nDiff-report: " +  configuration.reportPath().toAbsolutePath());
    }


    private static void printHeader() {
        System.out.println("\n");
        System.out.println("***********************************************");
        System.out.println("*** Database schema DIFF tool (poc version) ***");
        System.out.println("***********************************************");
    }

    private static void printFooter() {
        System.out.println("\n*************** Done. ***************\n");
    }

    private static void printUsage() {
        System.out.println("Usage: mvn exec:java -Dexec.args=\"--source=db|file [--config=path]\"");
        System.out.println("  --source=db    Compare source.db.* with target.db.* settings.");
        System.out.println("  --source=file  Compare source.file.* DDL with target.db.* settings.");
        System.out.println("  --config=PATH  Configuration file (default: config/connection.properties).");
    }

    private enum SourceType {DATABASE, FILE}

    private record Arguments(SourceType sourceType, Path configurationPath, boolean help) {
        private static Arguments parse(String[] args) {
            SourceType sourceType = null;
            Path configurationPath = Path.of("config", "connection.properties");
            for (String argument : args) {
                if ("--help".equals(argument) || "-h".equals(argument)) {
                    return new Arguments(null, configurationPath, true);
                }
                if (argument.startsWith("--source=")) {
                    sourceType = switch (argument.substring("--source=".length())) {
                        case "db" -> SourceType.DATABASE;
                        case "file" -> SourceType.FILE;
                        default -> throw new IllegalArgumentException("--source must be either 'db' or 'file'.");
                    };
                } else if (argument.startsWith("--config=")) {
                    configurationPath = Path.of(argument.substring("--config=".length()));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + argument);
                }
            }
            if (sourceType == null) {
                throw new IllegalArgumentException("Missing required argument: --source=db or --source=file.");
            }
            return new Arguments(sourceType, configurationPath, false);
        }
    }
}

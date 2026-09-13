package com.example.schemadiff.app;

import com.example.schemadiff.configuration.ConnectionPropertiesLoader;
import com.example.schemadiff.configuration.ToolConfiguration;
import com.example.schemadiff.core.model.SchemaSnapshot;
import com.example.schemadiff.infrastructure.file.PostgresDdlSchemaReader;
import com.example.schemadiff.infrastructure.jdbc.JdbcSchemaReader;
import com.example.schemadiff.infrastructure.report.MarkdownReportWriter;
import com.example.schemadiff.service.ReportGenerationService;
import com.example.schemadiff.service.SchemaComparisonService;
import com.example.schemadiff.service.SchemaIntrospectionService;

import java.nio.file.Path;

public final class SchemaDiffApplication {
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
                case FILE -> new PostgresDdlSchemaReader().read(configuration.sourceFile(), configuration.sourceFileSchema());
            };

            var comparison = new SchemaComparisonService().compare(source, target);
            new ReportGenerationService(new MarkdownReportWriter()).write(comparison, configuration.reportPath());
            System.out.printf("Compared %s to %s: %d difference(s).%nReport: %s%n", comparison.sourceName(),
                    comparison.targetName(), comparison.differences().size(), configuration.reportPath().toAbsolutePath());
        } catch (Exception exception) {
            System.err.println("Schema comparison failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: mvn exec:java -Dexec.args=\"--source=db|file [--config=path]\"");
        System.out.println("  --source=db    Compare source.db.* with target.db.* settings.");
        System.out.println("  --source=file  Compare source.file.* DDL with target.db.* settings.");
        System.out.println("  --config=PATH  Configuration file (default: config/connection.properties).");
    }

    private enum SourceType { DATABASE, FILE }

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

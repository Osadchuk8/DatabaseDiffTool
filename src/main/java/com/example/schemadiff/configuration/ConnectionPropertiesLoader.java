package com.example.schemadiff.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConnectionPropertiesLoader {
    private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    public ToolConfiguration load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Configuration file does not exist: " + path.toAbsolutePath());
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }

        return new ToolConfiguration(
                databaseSettings(properties, "source.db"),
                Path.of(required(properties, "source.file.path")),
                required(properties, "source.file.schema"),
                databaseSettings(properties, "target.db"),
                Path.of(required(properties, "report.path"))
        );
    }

    private DatabaseConnectionSettings databaseSettings(Properties properties, String prefix) {
        return new DatabaseConnectionSettings(
                required(properties, prefix + ".url"),
                required(properties, prefix + ".user"),
                resolve(properties.getProperty(prefix + ".password", ""), prefix + ".password"),
                required(properties, prefix + ".schema")
        );
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return resolve(value, key).trim();
    }

    private String resolve(String value, String propertyKey) {
        Matcher matcher = ENVIRONMENT_VARIABLE.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = System.getenv(variableName);
            if (replacement == null || replacement.isBlank()) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variableName + "' referenced by '" + propertyKey + "' is not set.");
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }
}

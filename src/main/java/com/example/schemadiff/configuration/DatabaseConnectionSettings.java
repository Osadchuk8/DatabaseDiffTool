package com.example.schemadiff.configuration;

public record DatabaseConnectionSettings(String url, String user, String password, String schema) {
}

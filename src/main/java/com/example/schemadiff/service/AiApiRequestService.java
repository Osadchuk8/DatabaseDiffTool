package com.example.schemadiff.service;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class AiApiRequestService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public AiApiRequestService(String apiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }


    public String generate(String prompt) throws IOException, InterruptedException {

        var requestBody = Map.of(
                "messages", new Object[]{
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                },
                "model", "openai/gpt-oss-120b",
                "temperature", 1,
                "max_completion_tokens", 2048,
                "top_p", 1,
                "stream", true,
                "reasoning_effort", "medium"
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<java.io.InputStream> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = new String(response.body().readAllBytes());
            throw new IOException(
                    "Groq API error " + response.statusCode() + ": " + error
            );
        }

        StringBuilder result = new StringBuilder();

        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                // SSE format: "data: {...}"
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring("data:".length()).trim();

                // End of stream
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonNode json = objectMapper.readTree(data);

                JsonNode content =
                        json.path("choices")
                                .path(0)
                                .path("delta")
                                .path("content");

                if (!content.isMissingNode() && !content.isNull()) {
                    result.append(content.asText());
                }
            }
        }

        return result.toString();
    }
}


package com.wherewear.backend.receiptimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Thin client for Gemini's generateContent API (vision + text), used to
 * read a receipt photo and extract line items. Gemini's free tier (Google
 * AI Studio) is the reason this is Gemini and not Claude/GPT-4V - keeps the
 * receipt-import feature free, matching the rest of the app's free-tier
 * integrations. See README "Receipt import" section for setup and quota.
 */
@Component
class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wherewear.gemini.api-key}")
    private String apiKey;

    @Value("${wherewear.gemini.model}")
    private String model;

    /**
     * Sends one prompt + image, asking for JSON-only output (Gemini's
     * structured-output mode), and returns the model's raw text response.
     */
    String generateJson(String prompt, byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Receipt import isn't configured yet (missing GEMINI_API_KEY). See README.");
        }

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode content = root.putArray("contents").addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", prompt);
        parts.addObject().putObject("inline_data")
                .put("mime_type", mimeType)
                .put("data", Base64.getEncoder().encodeToString(imageBytes));
        root.putObject("generationConfig").put("responseMimeType", "application/json");

        String url = BASE_URL + model + ":generateContent?key=" + encode(apiKey);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to reach Gemini", e);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Gemini's free tier routinely returns 503 ("model overloaded") /
        // 429 (rate limited) under load - both transient, so retry a couple
        // times with backoff before giving up.
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode responseRoot = objectMapper.readTree(response.body());
                    return responseRoot.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                }
                boolean transientError = response.statusCode() == 503 || response.statusCode() == 429;
                if (!transientError || attempt == maxAttempts) {
                    throw new ResponseStatusException(BAD_GATEWAY, "Gemini request failed: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(BAD_GATEWAY, "Failed to reach Gemini", e);
                }
                if (attempt == maxAttempts) {
                    throw new ResponseStatusException(BAD_GATEWAY, "Failed to reach Gemini", e);
                }
            }
            sleepBeforeRetry(attempt);
        }
        throw new ResponseStatusException(BAD_GATEWAY, "Gemini request failed after retries");
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(500L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to reach Gemini", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

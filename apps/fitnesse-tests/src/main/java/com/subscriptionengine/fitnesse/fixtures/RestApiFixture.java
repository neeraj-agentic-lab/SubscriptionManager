package com.subscriptionengine.fitnesse.fixtures;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic REST API fixture for FitNesse.
 * 
 * Handles any HTTP call — the FitNesse page controls method, URL, headers, body.
 * Keeps state (like auth token) across calls within the same script block.
 */
@Slf4j
public class RestApiFixture {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String baseUrl;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int statusCode;
    private String responseBody;
    private JsonNode responseJson;

    public RestApiFixture() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.baseUrl = System.getProperty("api.base.url",
                System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080/api"));
        this.headers.put("Content-Type", "application/json");
        log.info("RestApiFixture initialized with base URL: {}", baseUrl);
    }

    // ── Configuration ──────────────────────────────────────────

    public void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    public void setHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public void setAuthToken(String token) {
        this.headers.put("Authorization", "Bearer " + token);
    }

    public void setHeaderNameValue(String nameColonValue) {
        String[] parts = nameColonValue.split(":", 2);
        if (parts.length == 2) {
            this.headers.put(parts[0].trim(), parts[1].trim());
        }
    }

    public void clearHeaders() {
        this.headers.clear();
        this.headers.put("Content-Type", "application/json");
    }

    // ── HTTP Methods ───────────────────────────────────────────

    public void getFrom(String path) {
        execute("GET", path, null);
    }

    public void postTo(String path) {
        execute("POST", path, null);
    }

    public void postToWithBody(String path, String body) {
        execute("POST", path, body);
    }

    public void putToWithBody(String path, String jsonBody) {
        execute("PUT", path, jsonBody);
    }

    public void patchToWithBody(String path, String jsonBody) {
        execute("PATCH", path, jsonBody);
    }

    public void deleteFrom(String path) {
        execute("DELETE", path, null);
    }

    // ── Response Inspection ────────────────────────────────────

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        if (responseBody == null) return "";
        try {
            Object json = objectMapper.readValue(responseBody, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return responseBody;
        }
    }

    /**
     * Extract a value from the JSON response by key path.
     * Supports dot-notation: e.g. "user.email" or simple keys like "token".
     */
    public String jsonValue(String keyPath) {
        if (responseJson == null) return "";
        JsonNode node = responseJson;
        for (String key : keyPath.split("\\.")) {
            if (node == null) return "";
            node = node.get(key);
        }
        return node != null ? node.asText() : "";
    }

    public boolean responseContains(String text) {
        return responseBody != null && responseBody.contains(text);
    }

    public boolean statusCodeIs(int expected) {
        return statusCode == expected;
    }

    /**
     * Returns current timestamp — use in wiki pages to make values unique.
     * e.g. |$ts=|timestamp|
     * then use: {"slug":"simple-test-$ts"}
     */
    public String timestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    // ── Internal ───────────────────────────────────────────────

    /**
     * Strip FitNesse Slim symbol display notation.
     * Slim renders symbols as "$name->[value]" — we need just "value".
     */
    private String stripSymbolNotation(String input) {
        if (input == null) return null;
        return input.replaceAll("\\$\\w+->\\[([^\\]]*)]", "$1");
    }

    private boolean execute(String method, String path, String body) {
        path = stripSymbolNotation(path);
        body = stripSymbolNotation(body);
        String url = baseUrl + path;
        log.info("=== {} {} ===", method, url);
        if (body != null) log.info("Body: {}", body);

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(reqBuilder::header);

            switch (method) {
                case "GET"    -> reqBuilder.GET();
                case "POST"   -> reqBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                case "PUT"    -> reqBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                case "PATCH"  -> reqBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                case "DELETE" -> reqBuilder.DELETE();
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            statusCode = resp.statusCode();
            responseBody = resp.body();

            log.info("Status: {}", statusCode);
            log.info("Response: {}", responseBody);

            try {
                responseJson = objectMapper.readTree(responseBody);
            } catch (Exception e) {
                responseJson = null;
            }

            return statusCode >= 200 && statusCode < 300;

        } catch (Exception e) {
            log.error("{} {} failed: {}", method, url, e.getMessage(), e);
            statusCode = -1;
            responseBody = e.getMessage();
            responseJson = null;
            return false;
        }
    }
}

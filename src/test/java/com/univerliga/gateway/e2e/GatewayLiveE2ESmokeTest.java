package com.univerliga.gateway.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_E2E_SMOKE", matches = "true")
class GatewayLiveE2ESmokeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static HttpClient http;
    private static String baseUrl;

    @BeforeAll
    static void init() {
        http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        baseUrl = resolveBaseUrl();
    }

    @Test
    void managerFlowGatewayToCrmFeedbackAnalytics() throws Exception {
        String token = authToken("manager", "manager");

        Response people = get("/api/v1/crm/people?page=1&size=5", token);
        assertStatus(people, 200);
        assertTrue(jsonAt(people, "/data/items").isArray());

        Response tasks = get("/api/v1/crm/tasks?page=1&size=5", token);
        assertStatus(tasks, 200);
        assertTrue(jsonAt(tasks, "/data/items").isArray());

        Response categories = get("/api/v1/feedback/categories", token);
        assertStatus(categories, 200);
        assertNotNull(jsonAt(categories, "/data/items"));

        Response inbox = get("/api/v1/feedback/inbox?page=1&size=5", token);
        assertStatus(inbox, 200);
        assertTrue(jsonAt(inbox, "/data/items").isArray());

        String from = url("2026-01-01");
        String to = url("2026-12-31");

        Response summary = get("/api/v1/reports/summary?periodFrom=" + from + "&periodTo=" + to, token);
        assertStatus(summary, 200);
        assertNotNull(jsonAt(summary, "/data/kpis/responses"));

        Response topTags = get("/api/v1/reports/insights/top-tags?periodFrom=" + from + "&periodTo=" + to + "&limit=5", token);
        assertStatus(topTags, 200);
        assertTrue(jsonAt(topTags, "/data/topPositive").isArray());
        assertTrue(jsonAt(topTags, "/data/topNegative").isArray());
    }

    @Test
    void roleAccessContractsAreActual() throws Exception {
        String employee = authToken("employee", "employee");
        assertStatus(get("/api/v1/crm/people?page=1&size=5", employee), 403);
        assertStatus(get("/api/v1/reports/summary?periodFrom=2026-01-01&periodTo=2026-12-31", employee), 403);
        assertStatus(get("/api/v1/feedback/inbox?page=1&size=5", employee), 200);

        String hr = authToken("hr", "hr");
        assertStatus(get("/api/v1/feedback/raw?page=1&size=5", hr), 200);

        String admin = authToken("admin", "admin");
        assertStatus(get("/api/v1/feedback/raw?page=1&size=5", admin), 200);
    }

    private static String authToken(String username, String password) throws Exception {
        String body = MAPPER.writeValueAsString(Map.of("username", username, "password", password));
        Response response = post("/api/v1/auth/token", body, null);
        assertStatus(response, 200);
        String token = jsonAt(response, "/data/accessToken").asText();
        assertNotNull(token);
        assertTrue(!token.isBlank());
        return token;
    }

    private static Response get(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    private static Response post(String path, String jsonBody, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    private static JsonNode jsonAt(Response response, String pointer) throws IOException {
        return MAPPER.readTree(response.body).at(pointer);
    }

    private static void assertStatus(Response response, int expected) {
        assertEquals(expected, response.statusCode, "Unexpected status, body: " + response.body);
    }

    private static String resolveBaseUrl() {
        String fromProp = System.getProperty("gateway.baseUrl");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        String fromEnv = System.getenv("GATEWAY_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return "http://localhost:8080";
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Response(int statusCode, String body) {
    }
}

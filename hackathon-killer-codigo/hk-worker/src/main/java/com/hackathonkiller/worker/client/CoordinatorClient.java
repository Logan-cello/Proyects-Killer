package com.hackathonkiller.worker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Cliente HTTP del coordinador (polling HTTP, sin dependencias externas). */
public class CoordinatorClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public CoordinatorClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public JsonNode register(String name, String role, String adapterType, List<String> capabilities)
            throws IOException, InterruptedException {
        return post("/api/agents", Map.of(
                "name", name, "role", role, "adapterType", adapterType, "capabilities", capabilities));
    }

    public JsonNode heartbeat(String agentId) throws IOException, InterruptedException {
        return post("/api/agents/" + agentId + "/heartbeat", Map.of());
    }

    public JsonNode tasksFor(String agentId) throws IOException, InterruptedException {
        return get("/api/tasks?assignee=" + agentId);
    }

    public JsonNode transition(String taskId, String state) throws IOException, InterruptedException {
        return post("/api/tasks/" + taskId + "/transition", Map.of("state", state));
    }

    public JsonNode addEvidence(String taskId, String type, String summary, boolean passed)
            throws IOException, InterruptedException {
        return post("/api/tasks/" + taskId + "/evidence",
                Map.of("type", type, "summary", summary, "passed", passed));
    }

    public JsonNode setGit(String taskId, String branch, String commitSha)
            throws IOException, InterruptedException {
        return post("/api/tasks/" + taskId + "/git", Map.of("branch", branch, "commitSha", commitSha));
    }

    public JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return send(req);
    }

    private JsonNode post(String path, Object body) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return send(req);
    }

    private JsonNode send(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) {
            throw new IOException("Coordinador respondió " + res.statusCode() + ": " + res.body());
        }
        return mapper.readTree(res.body());
    }
}

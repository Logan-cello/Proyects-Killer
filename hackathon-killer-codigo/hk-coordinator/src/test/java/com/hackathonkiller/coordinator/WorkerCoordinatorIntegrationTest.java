package com.hackathonkiller.coordinator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathonkiller.coordinator.CoordinatorApplication;
import com.hackathonkiller.worker.adapter.AssistantAdapter;
import com.hackathonkiller.worker.client.CoordinatorClient;
import com.hackathonkiller.worker.git.GitService;
import com.hackathonkiller.worker.runner.CommandRunner;
import com.hackathonkiller.worker.runner.TaskExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CoordinatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerCoordinatorIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void tareaViaHttpSeAsignaSeEjecutaUnaVezYRegistraEvidencia() throws Exception {
        CoordinatorClient client = new CoordinatorClient("http://localhost:" + port);
        String suffix = Long.toString(System.nanoTime());
        String agentId = client.register("Integration Worker " + suffix, "DEVELOPER", "CLI", List.of("java"))
                .path("id").asText();

        JsonNode created = post("/api/tasks", mapper.createObjectNode()
                .put("title", "Probar flujo HTTP")
                .put("description", "Comprobar creación, asignación y ejecución")
                .set("acceptanceCriteria", mapper.valueToTree(List.of("Pruebas en verde"))));
        String taskId = created.path("id").asText();
        assertEquals("BACKLOG", created.path("state").asText());

        post("/api/tasks/" + taskId + "/ready", mapper.createObjectNode());
        post("/api/tasks/" + taskId + "/assign", mapper.createObjectNode().put("agentId", agentId));
        JsonNode assigned = client.tasksFor(agentId).get(0);
        assertEquals("ASSIGNED", assigned.path("state").asText());
        assertEquals(agentId, assigned.path("assigneeId").asText());

        AtomicInteger assistantRuns = new AtomicInteger();
        RecordingGit git = new RecordingGit();
        AssistantAdapter adapter = new AssistantAdapter() {
            @Override
            public String name() {
                return "integration-test";
            }

            @Override
            public Result execute(TaskPrompt prompt, Path workspace) {
                assistantRuns.incrementAndGet();
                return new Result(true, "Ejecución simulada de integración");
            }
        };
        TaskExecutor executor = new TaskExecutor(agentId, client, git, adapter, Path.of("."), null);
        executor.execute(assigned);

        JsonNode afterExecution = client.get("/api/tasks/" + taskId);
        assertEquals("READY_FOR_REVIEW", afterExecution.path("state").asText());
        assertEquals(agentId, afterExecution.path("assigneeId").asText());
        assertEquals(1, assistantRuns.get());
        assertTrue(afterExecution.path("evidence").toString().contains("abc123"));
        assertEquals("feature/" + taskId + "-" + agentId, git.createdBranch);

        assertThrows(IOException.class, () -> executor.execute(assigned));
        assertEquals(1, assistantRuns.get());
        assertEquals(1, git.commits);
        assertEquals(1, git.pushes);
    }

    private JsonNode post(String path, JsonNode body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() < 400,
                "POST " + path + " respondió " + response.statusCode() + ": " + response.body());
        return mapper.readTree(response.body());
    }

    private static final class RecordingGit extends GitService {
        private int commits;
        private int pushes;
        private String createdBranch;

        private RecordingGit() {
            super(Path.of("."), new CommandRunner(Duration.ofSeconds(1)));
        }

        @Override
        public void verifyWorkspace() { }

        @Override
        public void fetch() { }

        @Override
        public void createBranch(String branch) {
            createdBranch = branch;
        }

        @Override
        public String commitAll(String expectedBranch, String message) {
            commits++;
            return "abc123";
        }

        @Override
        public void push(String branch) {
            pushes++;
        }
    }
}

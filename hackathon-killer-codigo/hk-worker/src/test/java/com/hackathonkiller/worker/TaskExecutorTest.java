package com.hackathonkiller.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackathonkiller.worker.adapter.AssistantAdapter;
import com.hackathonkiller.worker.client.CoordinatorClient;
import com.hackathonkiller.worker.git.GitService;
import com.hackathonkiller.worker.runner.CommandRunner;
import com.hackathonkiller.worker.runner.TaskExecutor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutorTest {

    @Test
    void reclamaAntesDeEjecutarYDejaEvidenciaAlBloquearse() throws Exception {
        RecordingClient client = new RecordingClient();
        FakeGit git = new FakeGit();
        AssistantAdapter adapter = adapter(false);

        new TaskExecutor("agent-b", client, git, adapter, Path.of("."), null).execute(task());

        assertEquals(List.of("IN_PROGRESS", "BLOCKED"), client.transitions);
        assertEquals(1, client.evidence.size());
        assertEquals("LOG", client.evidence.getFirst().type());
        assertEquals(1, git.fetches);
        assertEquals(0, git.commits);
    }

    @Test
    void falloDePruebasBloqueaLaTareaYNoHaceCommit() throws Exception {
        RecordingClient client = new RecordingClient();
        FakeGit git = new FakeGit();

        new TaskExecutor("agent-b", client, git, adapter(true), Path.of("."),
                "hk-command-that-does-not-exist").execute(task());

        assertEquals(List.of("IN_PROGRESS", "BLOCKED"), client.transitions);
        assertEquals("TEST_RESULT", client.evidence.getFirst().type());
        assertFalse(client.evidence.getFirst().passed());
        assertTrue(client.evidence.getFirst().summary().contains("127"));
        assertEquals(0, git.commits);
        assertEquals(0, git.pushes);
    }

    @Test
    void rechazoDeReclamoImpideCualquierEjecucion() {
        RecordingClient client = new RecordingClient();
        client.rejectClaim = true;
        FakeGit git = new FakeGit();
        int[] assistantCalls = {0};

        assertThrows(IOException.class, () -> new TaskExecutor("agent-b", client, git,
                adapter(true, assistantCalls), Path.of("."), null).execute(task()));

        assertEquals(0, git.fetches);
        assertEquals(0, assistantCalls[0]);
    }

    @Test
    void soloPasaARevisionTrasPrepararRamaYRegistrarEjecucion() throws Exception {
        RecordingClient client = new RecordingClient();
        FakeGit git = new FakeGit();

        new TaskExecutor("agent-b", client, git, adapter(true), Path.of("."), null).execute(task());

        assertEquals(List.of("IN_PROGRESS", "READY_FOR_REVIEW"), client.transitions);
        assertEquals("feature/HK-001-agent-b", git.createdBranch);
        assertEquals(1, git.commits);
        assertEquals(1, git.pushes);
        assertTrue(client.evidence.stream().anyMatch(e -> "COMMIT".equals(e.type()) && e.passed()));
    }

    @Test
    void runnerDevuelveErrorSiNoPuedeIniciarElComando() {
        CommandRunner.Result result = new CommandRunner(Duration.ofSeconds(1))
                .run(Path.of("."), List.of("hk-command-that-does-not-exist"));

        assertEquals(127, result.exitCode());
        assertFalse(result.ok());
        assertTrue(result.output().contains("No se pudo iniciar"));
    }

    @Test
    void runnerAplicaElTiempoLimiteAlProceso() {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java")
                .toString();
        CommandRunner.Result result = new CommandRunner(Duration.ofMillis(200))
                .run(Path.of("."), List.of(javaExecutable, "-cp", System.getProperty("java.class.path"),
                        CommandRunnerTimeoutProcess.class.getName()));

        assertEquals(124, result.exitCode());
        assertTrue(result.output().contains("Timeout"));
    }

    private static JsonNode task() {
        ObjectNode task = new ObjectMapper().createObjectNode();
        task.put("id", "HK-001");
        task.put("title", "Probar Agent B");
        task.put("description", "Tarea de prueba");
        ArrayNode criteria = task.putArray("acceptanceCriteria");
        criteria.add("Pruebas en verde");
        return task;
    }

    private static AssistantAdapter adapter(boolean success) {
        return adapter(success, new int[]{0});
    }

    private static AssistantAdapter adapter(boolean success, int[] calls) {
        return new AssistantAdapter() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public Result execute(TaskPrompt prompt, Path workspace) {
                calls[0]++;
                return new Result(success, success ? "OK" : "fallo simulado");
            }
        };
    }

    private static final class RecordingClient extends CoordinatorClient {
        private final List<String> transitions = new ArrayList<>();
        private final List<EvidenceCall> evidence = new ArrayList<>();
        private boolean rejectClaim;

        private RecordingClient() {
            super("http://localhost");
        }

        @Override
        public JsonNode transition(String taskId, String state) throws IOException {
            if (rejectClaim && "IN_PROGRESS".equals(state)) {
                throw new IOException("reclamo rechazado");
            }
            transitions.add(state);
            return NullNode.instance;
        }

        @Override
        public JsonNode addEvidence(String taskId, String type, String summary, boolean passed) {
            evidence.add(new EvidenceCall(type, summary, passed));
            return NullNode.instance;
        }

        @Override
        public JsonNode setGit(String taskId, String branch, String commitSha) {
            return NullNode.instance;
        }
    }

    private record EvidenceCall(String type, String summary, boolean passed) { }

    private static final class FakeGit extends GitService {
        private int fetches;
        private int commits;
        private int pushes;
        private String createdBranch;

        private FakeGit() {
            super(Path.of("."), new CommandRunner(Duration.ofSeconds(1)));
        }

        @Override
        public void verifyWorkspace() { }

        @Override
        public void fetch() {
            fetches++;
        }

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

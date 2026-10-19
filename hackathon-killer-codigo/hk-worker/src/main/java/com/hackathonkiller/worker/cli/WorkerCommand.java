package com.hackathonkiller.worker.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathonkiller.worker.adapter.AssistantAdapter;
import com.hackathonkiller.worker.adapter.CliAdapter;
import com.hackathonkiller.worker.adapter.ManualAdapter;
import com.hackathonkiller.worker.client.CoordinatorClient;
import com.hackathonkiller.worker.git.GitService;
import com.hackathonkiller.worker.runner.CommandRunner;
import com.hackathonkiller.worker.runner.TaskExecutor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Worker local: se registra, mantiene heartbeat y ejecuta las tareas que le asignen.
 *
 * Ejemplo (Agente B con asistente manual):
 *   java -jar hk-worker.jar --name "Agent B" --role DEVELOPER --adapter MANUAL --repo C:\ruta\al\repo
 */
@Command(name = "hk-worker", mixinStandardHelpOptions = true, version = "0.1.0",
        description = "Worker local de Hackathon Killer")
public class WorkerCommand implements Callable<Integer> {

    @Option(names = "--coordinator", defaultValue = "http://localhost:8080", description = "URL del coordinador")
    String coordinatorUrl;

    @Option(names = "--name", required = true, description = "Nombre del agente, p. ej. \"Agent B\"")
    String name;

    @Option(names = "--role", required = true, description = "STRATEGY_ARCHITECT | DEVELOPER | QUALITY_INTEGRATION")
    String role;

    @Option(names = "--adapter", defaultValue = "MANUAL", description = "MANUAL | CLI")
    String adapter;

    @Option(names = "--assistant-cmd", description = "Comando del asistente (solo adaptador CLI)")
    String assistantCmd;

    @Option(names = "--repo", defaultValue = ".", description = "Ruta del repositorio local")
    Path repo;

    @Option(names = "--test-cmd", description = "Comando de pruebas, p. ej. \"mvn -q test\"")
    String testCmd;

    @Option(names = "--capabilities", split = ",", description = "Capacidades declaradas")
    List<String> capabilities = List.of();

    @Option(names = "--poll-seconds", defaultValue = "10")
    int pollSeconds;

    @Option(names = "--once", description = "Procesar una sola vuelta y salir")
    boolean once;

    @Override
    public Integer call() throws Exception {
        CoordinatorClient client = new CoordinatorClient(coordinatorUrl);
        JsonNode me = client.register(name, role, adapter.toUpperCase(), capabilities);
        String agentId = me.get("id").asText();
        System.out.println("Registrado como " + agentId + " (" + role + ", adaptador " + adapter + ")");

        AssistantAdapter assistant = "CLI".equalsIgnoreCase(adapter)
                ? new CliAdapter(assistantCmd, Duration.ofMinutes(30))
                : new ManualAdapter();
        GitService git = new GitService(repo, new CommandRunner(Duration.ofMinutes(2)));
        TaskExecutor executor = new TaskExecutor(agentId, client, git, assistant, repo, testCmd);

        do {
            try {
                client.heartbeat(agentId);
                for (JsonNode task : client.tasksFor(agentId)) {
                    if ("ASSIGNED".equals(task.path("state").asText())) {
                        System.out.println("Ejecutando " + task.get("id").asText() + ": " + task.path("title").asText());
                        executor.execute(task);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error en el ciclo del worker: " + e.getMessage());
            }
            if (!once) {
                Thread.sleep(pollSeconds * 1000L);
            }
        } while (!once);
        return 0;
    }
}

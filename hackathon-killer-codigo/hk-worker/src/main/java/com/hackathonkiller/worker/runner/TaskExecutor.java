package com.hackathonkiller.worker.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathonkiller.worker.adapter.AssistantAdapter;
import com.hackathonkiller.worker.client.CoordinatorClient;
import com.hackathonkiller.worker.git.GitService;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ejecuta una tarea asignada: rama -> asistente -> tests -> commit -> push -> evidencia. */
public class TaskExecutor {

    private final String agentId;
    private final CoordinatorClient client;
    private final GitService git;
    private final AssistantAdapter adapter;
    private final Path repoDir;
    private final String testCommand;
    private final CommandRunner testRunner = new CommandRunner(Duration.ofMinutes(10));

    public TaskExecutor(String agentId, CoordinatorClient client, GitService git,
                        AssistantAdapter adapter, Path repoDir, String testCommand) {
        this.agentId = agentId;
        this.client = client;
        this.git = git;
        this.adapter = adapter;
        this.repoDir = repoDir;
        this.testCommand = testCommand;
    }

    public void execute(JsonNode task) throws Exception {
        String taskId = task.get("id").asText();
        List<String> criteria = new ArrayList<>();
        task.withArray("acceptanceCriteria").forEach(n -> criteria.add(n.asText()));

        client.transition(taskId, "IN_PROGRESS");
        String branch = GitService.branchName("feature", taskId, agentId);
        String failureType = "LOG";
        String failureSummary = null;
        boolean interrupted = false;
        try {
            git.verifyWorkspace();
            git.fetch();
            git.createBranch(branch);

            AssistantAdapter.Result result = adapter.execute(
                    new AssistantAdapter.TaskPrompt(taskId, task.path("title").asText(),
                            task.path("description").asText(null), criteria), repoDir);
            if (!result.success()) {
                throw new TaskFailureException("LOG", result.summary());
            }

            if (testCommand != null && !testCommand.isBlank()) {
                CommandRunner.Result tests = testRunner.run(repoDir, Arrays.asList(testCommand.trim().split("\\s+")));
                if (!tests.ok()) {
                    throw new TaskFailureException("TEST_RESULT",
                            "Las pruebas fallaron (código " + tests.exitCode() + "): " + tests.output());
                }
                client.addEvidence(taskId, "TEST_RESULT", "Tests OK (código 0)", true);
            }

            String sha = git.commitAll(branch,
                    GitService.commitMessage("feat", taskId, task.path("title").asText(), agentId));
            git.push(branch);
            client.setGit(taskId, branch, sha);
            client.addEvidence(taskId, "COMMIT", sha, true);
            client.transition(taskId, "READY_FOR_REVIEW");
        } catch (TaskFailureException e) {
            failureType = e.evidenceType;
            failureSummary = e.getMessage();
        } catch (Exception e) {
            failureSummary = "Error de ejecución: " + failureSummary(e);
            if (e instanceof InterruptedException) {
                interrupted = true;
            }
        }
        if (failureSummary != null) {
            block(taskId, failureType, failureSummary);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void block(String taskId, String evidenceType, String summary) throws Exception {
        client.addEvidence(taskId, evidenceType, summary, false);
        client.transition(taskId, "BLOCKED");
    }

    private static String failureSummary(Exception e) {
        String detail = e.getMessage();
        String summary = e.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? "" : ": " + detail);
        return summary.length() <= 4000 ? summary : summary.substring(0, 4000);
    }

    private static final class TaskFailureException extends Exception {

        private final String evidenceType;

        private TaskFailureException(String evidenceType, String message) {
            super(message);
            this.evidenceType = evidenceType;
        }
    }
}

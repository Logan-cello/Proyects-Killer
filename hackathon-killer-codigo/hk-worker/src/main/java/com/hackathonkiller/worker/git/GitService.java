package com.hackathonkiller.worker.git;

import com.hackathonkiller.worker.runner.CommandRunner;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Operaciones Git permitidas para el worker. Reglas del proyecto:
 * nunca trabajar sobre main, nunca force push, una rama por tarea.
 */
public class GitService {

    private static final Set<String> PROTECTED_BRANCHES = Set.of("main", "master");

    private final Path repoDir;
    private final CommandRunner runner;

    public GitService(Path repoDir, CommandRunner runner) {
        this.repoDir = repoDir;
        this.runner = runner;
    }

    /** feature/HK-001-agent-b */
    public static String branchName(String kind, String taskId, String agentId) {
        return kind + "/" + taskId + "-" + agentId;
    }

    /** feat(HK-001): implement task orchestration [agent-b] */
    public static String commitMessage(String type, String taskId, String summary, String agentId) {
        return type + "(" + taskId + "): " + summary + " [" + agentId + "]";
    }

    public void fetch() {
        git("fetch", "--all", "--prune");
    }

    public void verifyWorkspace() {
        try {
            Path configuredRoot = repoDir.toRealPath();
            Path gitRoot = Path.of(git("rev-parse", "--show-toplevel").trim()).toRealPath();
            if (!configuredRoot.equals(gitRoot)) {
                throw new IllegalStateException(
                        "La ruta configurada no es la raíz del repositorio Git: " + configuredRoot);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo resolver la ruta del repositorio: " + repoDir, e);
        }
        String changes = status();
        if (!changes.isBlank()) {
            throw new IllegalStateException(
                    "El repositorio de trabajo tiene cambios locales; no se integrarán en la tarea: " + changes);
        }
    }

    public void createBranch(String branch) {
        assertNotProtected(branch);
        git("checkout", "-b", branch);
    }

    /** Hace add + commit y devuelve el SHA del nuevo commit. */
    public String commitAll(String expectedBranch, String message) {
        assertCurrentWorkBranch(expectedBranch);
        git("add", "-A");
        git("commit", "-m", message);
        return git("rev-parse", "HEAD").trim();
    }

    /** Push normal (sin --force). */
    public void push(String branch) {
        assertCurrentWorkBranch(branch);
        git("push", "-u", "origin", branch);
    }

    public String currentBranch() {
        return git("rev-parse", "--abbrev-ref", "HEAD").trim();
    }

    public String status() {
        return git("status", "--short");
    }

    static void assertNotProtected(String branch) {
        if (branch == null || PROTECTED_BRANCHES.contains(branch.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Operación no permitida sobre la rama protegida: " + branch);
        }
    }

    private void assertCurrentWorkBranch(String expectedBranch) {
        assertNotProtected(expectedBranch);
        String actualBranch = currentBranch();
        if (!expectedBranch.equals(actualBranch)) {
            throw new IllegalStateException(
                    "La rama actual no coincide con la rama de tarea esperada: "
                            + actualBranch + " != " + expectedBranch);
        }
    }

    private String git(String... args) {
        for (String a : args) {
            if (a.equals("--force") || a.equals("-f") || a.startsWith("--force-with-lease")) {
                throw new IllegalStateException("force push requiere autorización humana");
            }
        }
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        CommandRunner.Result r = runner.run(repoDir, cmd);
        if (!r.ok()) {
            throw new IllegalStateException("git " + String.join(" ", args) + " falló: " + r.output());
        }
        return r.output();
    }
}

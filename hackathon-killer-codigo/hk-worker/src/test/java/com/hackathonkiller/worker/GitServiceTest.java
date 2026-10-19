package com.hackathonkiller.worker;

import com.hackathonkiller.worker.git.GitService;
import com.hackathonkiller.worker.runner.CommandRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generaNombreDeRamaYCommitSegunElFlujo() {
        assertEquals("feature/HK-001-agent-b", GitService.branchName("feature", "HK-001", "agent-b"));
        assertEquals("feat(HK-001): implement task orchestration [agent-b]",
                GitService.commitMessage("feat", "HK-001", "implement task orchestration", "agent-b"));
    }

    @Test
    void noPermiteTrabajarSobreMain() {
        GitService git = new GitService(Path.of("."),
                new com.hackathonkiller.worker.runner.CommandRunner(Duration.ofSeconds(5)));
        assertThrows(IllegalStateException.class, () -> git.createBranch("main"));
        assertThrows(IllegalStateException.class, () -> git.push("master"));
    }

    @Test
    void noPermiteCommitNiPushSiLaRamaActualNoEsLaRamaDeTarea() throws IOException, InterruptedException {
        Process init = new ProcessBuilder("git", "init", "--initial-branch=main", tempDir.toString())
                .redirectErrorStream(true)
                .start();
        assertEquals(0, init.waitFor(), "git init debe completar correctamente");
        Process commit = new ProcessBuilder("git", "-C", tempDir.toString(),
                "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                "commit", "--allow-empty", "-m", "initial")
                .redirectErrorStream(true)
                .start();
        assertEquals(0, commit.waitFor(), "el commit inicial de prueba debe completar correctamente");
        GitService git = new GitService(tempDir, new CommandRunner(Duration.ofSeconds(5)));

        assertEquals("main", git.currentBranch());
        assertThrows(IllegalStateException.class,
                () -> git.commitAll("feature/HK-001-agent-b", "feat(HK-001): test"));
        assertThrows(IllegalStateException.class, () -> git.push("main"));
    }
}

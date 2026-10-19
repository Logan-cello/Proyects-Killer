package com.hackathonkiller.worker.adapter;

import com.hackathonkiller.worker.runner.CommandRunner;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adaptador CLI genérico: ejecuta el comando configurado del asistente pasando el prompt
 * de la tarea como último argumento. Ejemplo: --assistant-cmd "mi-asistente --non-interactive -p".
 */
public class CliAdapter implements AssistantAdapter {

    private final List<String> baseCommand;
    private final CommandRunner runner;

    public CliAdapter(String assistantCommand, Duration timeout) {
        if (assistantCommand == null || assistantCommand.isBlank()) {
            throw new IllegalArgumentException("--assistant-cmd es obligatorio para el adaptador CLI");
        }
        this.baseCommand = Arrays.asList(assistantCommand.trim().split("\\s+"));
        this.runner = new CommandRunner(timeout);
    }

    @Override
    public String name() {
        return "cli";
    }

    @Override
    public Result execute(TaskPrompt prompt, Path workspace) {
        List<String> cmd = new ArrayList<>(baseCommand);
        cmd.add(prompt.render());
        CommandRunner.Result r = runner.run(workspace, cmd);
        String tail = r.output().length() > 500 ? r.output().substring(r.output().length() - 500) : r.output();
        return new Result(r.ok(), r.ok() ? "Asistente terminó OK" : "Asistente falló (" + r.exitCode() + "): " + tail);
    }
}

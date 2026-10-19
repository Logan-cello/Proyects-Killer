package com.hackathonkiller.worker.adapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

/** Modo manual asistido: muestra la tarea y espera a que la persona confirme que terminó. */
public class ManualAdapter implements AssistantAdapter {

    @Override
    public String name() {
        return "manual";
    }

    @Override
    public Result execute(TaskPrompt prompt, Path workspace) throws Exception {
        System.out.println("=== TAREA PARA EL ASISTENTE (modo manual) ===");
        System.out.println(prompt.render());
        System.out.println("Workspace: " + workspace.toAbsolutePath());
        System.out.println("Pega el prompt en tu asistente, aplica los cambios y escribe:");
        System.out.println("  ok      -> la tarea está hecha");
        System.out.println("  bloqueo -> no se pudo completar");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line = in.readLine();
        boolean ok = line != null && line.trim().equalsIgnoreCase("ok");
        return new Result(ok, ok ? "Confirmado manualmente" : "Marcado como bloqueado manualmente");
    }
}

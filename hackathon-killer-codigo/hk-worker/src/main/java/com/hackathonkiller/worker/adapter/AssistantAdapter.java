package com.hackathonkiller.worker.adapter;

/**
 * Contrato común para invocar al asistente de IA local, sea cual sea su proveedor.
 * Implementaciones: CLI, MANUAL (ahora); API/SDK, MCP, ACP (después).
 */
public interface AssistantAdapter {

    record TaskPrompt(String taskId, String title, String description, java.util.List<String> acceptanceCriteria) {
        public String render() {
            StringBuilder sb = new StringBuilder();
            sb.append("Tarea ").append(taskId).append(": ").append(title).append("\n\n");
            if (description != null) {
                sb.append(description).append("\n\n");
            }
            sb.append("Criterios de aceptación:\n");
            for (String c : acceptanceCriteria) {
                sb.append("- ").append(c).append("\n");
            }
            return sb.toString();
        }
    }

    record Result(boolean success, String summary) { }

    String name();

    /** Ejecuta la tarea en el workspace ya preparado (rama creada) y devuelve el resultado. */
    Result execute(TaskPrompt prompt, java.nio.file.Path workspace) throws Exception;
}

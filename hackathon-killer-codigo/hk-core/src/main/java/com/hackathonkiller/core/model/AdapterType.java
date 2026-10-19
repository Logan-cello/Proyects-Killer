package com.hackathonkiller.core.model;

/** Nivel de integración con el asistente de IA local de cada worker. */
public enum AdapterType {
    API_SDK,
    CLI,
    MCP,
    ACP,
    /** Fallback: una persona ejecuta el asistente y confirma el resultado. */
    MANUAL
}

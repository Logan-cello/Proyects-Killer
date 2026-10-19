package com.hackathonkiller.core.exception;

/** Se lanza cuando una transición de tarea viola la máquina de estados o sus reglas. */
public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(String message) {
        super(message);
    }
}

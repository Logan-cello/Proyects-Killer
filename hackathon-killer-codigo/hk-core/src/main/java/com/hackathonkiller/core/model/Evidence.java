package com.hackathonkiller.core.model;

import java.time.Instant;

/**
 * Evidencia verificable de que algo se hizo (resultado de tests, commit, PR...).
 *
 * @param type    TEST_RESULT, COMMIT, PR, LOG...
 * @param summary descripción corta
 * @param passed  si la evidencia es favorable (p. ej. tests en verde)
 * @param at      cuándo se registró
 */
public record Evidence(String type, String summary, boolean passed, Instant at) {

    public Evidence {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type es obligatorio");
        }
        if (at == null) {
            at = Instant.now();
        }
    }

    public static Evidence of(String type, String summary, boolean passed) {
        return new Evidence(type, summary, passed, Instant.now());
    }
}

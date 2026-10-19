package com.hackathonkiller.core.state;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados de las tareas.
 *
 * <pre>
 * BACKLOG -> READY -> ASSIGNED -> IN_PROGRESS -> READY_FOR_REVIEW -> VALIDATING -> PASSED -> MERGED -> DONE
 *                                    |  ^                                  |
 *                                 BLOCKED                         FAILED / CHANGES_REQUESTED -> IN_PROGRESS
 * </pre>
 *
 * Regla clave: nunca se puede ir de IN_PROGRESS a DONE.
 */
public enum TaskState {
    BACKLOG,
    READY,
    ASSIGNED,
    IN_PROGRESS,
    BLOCKED,
    READY_FOR_REVIEW,
    VALIDATING,
    FAILED,
    CHANGES_REQUESTED,
    PASSED,
    MERGED,
    DONE,
    CANCELLED;

    public Set<TaskState> allowedNext() {
        return switch (this) {
            case BACKLOG -> EnumSet.of(READY, CANCELLED);
            case READY -> EnumSet.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, READY, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(BLOCKED, READY_FOR_REVIEW, CANCELLED);
            case BLOCKED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case READY_FOR_REVIEW -> EnumSet.of(VALIDATING, CANCELLED);
            case VALIDATING -> EnumSet.of(PASSED, FAILED, CHANGES_REQUESTED);
            case FAILED, CHANGES_REQUESTED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case PASSED -> EnumSet.of(MERGED);
            case MERGED -> EnumSet.of(DONE);
            case DONE, CANCELLED -> EnumSet.noneOf(TaskState.class);
        };
    }

    public boolean canTransitionTo(TaskState next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }
}

package com.hackathonkiller.core.state;

import com.hackathonkiller.core.exception.InvalidTransitionException;
import com.hackathonkiller.core.model.Evidence;
import com.hackathonkiller.core.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskStateTest {

    private Task newTask() {
        return new Task("HK-001", "Demo", "desc", List.of("Compila y pasa tests"));
    }

    @Test
    void inProgressNuncaPasaDirectoADone() {
        assertFalse(TaskState.IN_PROGRESS.canTransitionTo(TaskState.DONE));
    }

    @Test
    void tareaSinCriteriosDeAceptacionNoSeCrea() {
        assertThrows(IllegalArgumentException.class, () -> new Task("HK-X", "t", "d", List.of()));
    }

    @Test
    void asignarSinResponsableFalla() {
        Task t = newTask();
        t.transitionTo(TaskState.READY);
        assertThrows(InvalidTransitionException.class, () -> t.transitionTo(TaskState.ASSIGNED));
    }

    @Test
    void asignacionInvalidaNoGuardaElResponsable() {
        Task t = newTask();
        assertThrows(InvalidTransitionException.class, () -> t.assignTo("agent-b"));
        assertNull(t.getAssigneeId());
        assertEquals(TaskState.BACKLOG, t.getState());
    }

    @Test
    void pedirRevisionSinEvidenciaFalla() {
        Task t = newTask();
        t.transitionTo(TaskState.READY);
        t.assignTo("agent-b");
        t.transitionTo(TaskState.IN_PROGRESS);
        assertThrows(InvalidTransitionException.class, () -> t.transitionTo(TaskState.READY_FOR_REVIEW));
    }

    @Test
    void aprobarSinPruebasEnVerdeFalla() {
        Task t = llegarAValidating();
        assertThrows(InvalidTransitionException.class, () -> t.transitionTo(TaskState.PASSED));
    }

    @Test
    void ultimaEvidenciaDePruebasDebeSerExitosaParaAprobar() {
        Task t = llegarAValidating();
        t.addEvidence(Evidence.of("TEST_RESULT", "10 OK", true));
        t.addEvidence(Evidence.of("TEST_RESULT", "1 falló", false));
        assertThrows(InvalidTransitionException.class, () -> t.transitionTo(TaskState.PASSED));
        assertEquals(TaskState.VALIDATING, t.getState());
    }

    @Test
    void flujoCompletoFelizLlegaADone() {
        Task t = llegarAValidating();
        t.addEvidence(Evidence.of("TEST_RESULT", "12 tests OK", true));
        t.transitionTo(TaskState.PASSED);
        t.transitionTo(TaskState.MERGED);
        t.transitionTo(TaskState.DONE);
        assertEquals(TaskState.DONE, t.getState());
        assertTrue(t.getState().isTerminal());
    }

    @Test
    void rondasDeRevisionTienenLimite() {
        Task t = llegarAValidating(); // ronda 1
        for (int ronda = 2; ronda <= Task.MAX_REVIEW_ROUNDS; ronda++) {
            t.transitionTo(TaskState.FAILED);
            t.transitionTo(TaskState.IN_PROGRESS);
            t.transitionTo(TaskState.READY_FOR_REVIEW);
            t.transitionTo(TaskState.VALIDATING);
        }
        // se agotaron las rondas: una nueva revisión debe rechazarse
        t.transitionTo(TaskState.FAILED);
        t.transitionTo(TaskState.IN_PROGRESS);
        t.transitionTo(TaskState.READY_FOR_REVIEW);
        assertThrows(InvalidTransitionException.class, () -> t.transitionTo(TaskState.VALIDATING));
        assertEquals(Task.MAX_REVIEW_ROUNDS, t.getReviewRounds());
    }

    @Test
    void tareaRechazadaVuelveALaCola() {
        Task t = newTask();
        t.transitionTo(TaskState.READY);
        t.assignTo("agent-b");
        t.transitionTo(TaskState.READY);
        assertNull(t.getAssigneeId());
    }

    private Task llegarAValidating() {
        Task t = newTask();
        t.transitionTo(TaskState.READY);
        t.assignTo("agent-b");
        t.transitionTo(TaskState.IN_PROGRESS);
        t.addEvidence(Evidence.of("COMMIT", "abc123", true));
        t.transitionTo(TaskState.READY_FOR_REVIEW);
        t.setReviewerId("agent-c");
        t.transitionTo(TaskState.VALIDATING);
        return t;
    }
}

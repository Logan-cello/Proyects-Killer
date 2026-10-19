package com.hackathonkiller.coordinator;

import com.hackathonkiller.core.exception.InvalidTransitionException;
import com.hackathonkiller.core.message.Message;
import com.hackathonkiller.core.message.MessageType;
import com.hackathonkiller.core.model.AdapterType;
import com.hackathonkiller.core.model.AgentRole;
import com.hackathonkiller.core.model.AgentStatus;
import com.hackathonkiller.core.model.Task;
import com.hackathonkiller.core.state.TaskState;
import com.hackathonkiller.coordinator.repository.InMemoryAgentRepository;
import com.hackathonkiller.coordinator.repository.InMemoryMessageRepository;
import com.hackathonkiller.coordinator.repository.InMemoryTaskRepository;
import com.hackathonkiller.coordinator.service.AgentService;
import com.hackathonkiller.coordinator.service.MessageService;
import com.hackathonkiller.coordinator.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private AgentService agents;
    private TaskService tasks;
    private MessageService messages;

    @BeforeEach
    void setUp() {
        messages = new MessageService(new InMemoryMessageRepository());
        agents = new AgentService(new InMemoryAgentRepository(), messages, 30);
        tasks = new TaskService(new InMemoryTaskRepository(), agents, messages, 2);
        agents.register("Agent A", AgentRole.STRATEGY_ARCHITECT, AdapterType.MANUAL, List.of("design"));
        agents.register("Agent B", AgentRole.DEVELOPER, AdapterType.CLI, List.of("java"));
        agents.register("Agent C", AgentRole.QUALITY_INTEGRATION, AdapterType.CLI, List.of("test"));
    }

    @Test
    void registraTresAgentes() {
        assertEquals(3, agents.list().size());
        assertNotNull(agents.get("agent-b"));
    }

    @Test
    void generaIdsSecuenciales() {
        Task t1 = tasks.create("Uno", "d", List.of("ok"));
        Task t2 = tasks.create("Dos", "d", List.of("ok"));
        assertEquals("HK-001", t1.getId());
        assertEquals("HK-002", t2.getId());
    }

    @Test
    void flujoCompletoConRevisionIndependiente() {
        Task t = tasks.create("Feature", "d", List.of("tests en verde"));
        tasks.markReady(t.getId());
        tasks.assign(t.getId(), "agent-b");
        tasks.transition(t.getId(), TaskState.IN_PROGRESS, null);
        tasks.addEvidence(t.getId(), "COMMIT", "abc123", true);
        tasks.transition(t.getId(), TaskState.READY_FOR_REVIEW, null);

        // el autor no puede revisar su propio trabajo
        assertThrows(InvalidTransitionException.class,
                () -> tasks.transition(t.getId(), TaskState.VALIDATING, "agent-b"));

        tasks.transition(t.getId(), TaskState.VALIDATING, "agent-c");
        assertThrows(InvalidTransitionException.class,
                () -> tasks.transition(t.getId(), TaskState.PASSED, null)); // sin tests

        tasks.addEvidence(t.getId(), "TEST_RESULT", "10 OK", true);
        tasks.transition(t.getId(), TaskState.PASSED, null);
        tasks.transition(t.getId(), TaskState.MERGED, null);
        Task done = tasks.transition(t.getId(), TaskState.DONE, null);
        assertEquals(TaskState.DONE, done.getState());
    }

    @Test
    void asignaTareaListaAUnAgenteConectadoYConservaResponsable() {
        Task task = tasks.create("Probar Agent B", "d", List.of("ok"));
        tasks.markReady(task.getId());

        Task assigned = tasks.assign(task.getId(), "agent-b");

        assertEquals(AgentStatus.ONLINE, agents.get("agent-b").getStatus());
        assertEquals(TaskState.ASSIGNED, assigned.getState());
        assertEquals("agent-b", tasks.get(task.getId()).getAssigneeId());
    }

    @Test
    void asignacionInvalidaNoDejaMutacionParcialEnElRepositorio() {
        Task task = tasks.create("No reasignar", "d", List.of("ok"));
        tasks.markReady(task.getId());
        tasks.assign(task.getId(), "agent-b");
        tasks.transition(task.getId(), TaskState.IN_PROGRESS, null);

        assertThrows(InvalidTransitionException.class,
                () -> tasks.assign(task.getId(), "agent-c"));

        Task unchanged = tasks.get(task.getId());
        assertEquals(TaskState.IN_PROGRESS, unchanged.getState());
        assertEquals("agent-b", unchanged.getAssigneeId());
    }

    @Test
    void transicionInvalidaNoPersisteReviewerNiPermiteReclamarDosVeces() {
        Task task = tasks.create("Reclamo único", "d", List.of("ok"));
        tasks.markReady(task.getId());
        tasks.assign(task.getId(), "agent-b");
        tasks.transition(task.getId(), TaskState.IN_PROGRESS, null);

        assertThrows(InvalidTransitionException.class,
                () -> tasks.transition(task.getId(), TaskState.VALIDATING, "agent-c"));
        assertNull(tasks.get(task.getId()).getReviewerId());
        assertThrows(InvalidTransitionException.class,
                () -> tasks.transition(task.getId(), TaskState.IN_PROGRESS, null));

        tasks.addEvidence(task.getId(), "COMMIT", "abc123", true);
        tasks.transition(task.getId(), TaskState.READY_FOR_REVIEW, null);
        assertThrows(InvalidTransitionException.class,
                () -> tasks.transition(task.getId(), TaskState.VALIDATING, null));
        assertEquals(TaskState.READY_FOR_REVIEW, tasks.get(task.getId()).getState());
    }

    @Test
    void noAsignaMasTareasActivasQueElLimite() {
        for (int i = 0; i < 3; i++) {
            Task t = tasks.create("T" + i, "d", List.of("ok"));
            tasks.markReady(t.getId());
        }
        tasks.assign("HK-001", "agent-b");
        tasks.assign("HK-002", "agent-b");
        assertThrows(InvalidTransitionException.class, () -> tasks.assign("HK-003", "agent-b"));
    }

    @Test
    void ignoraMensajesDuplicados() {
        Message m = Message.simple("agent-a", "agent-b", MessageType.QUESTION, null, Map.of("q", "?"));
        assertTrue(messages.publish(m));
        assertFalse(messages.publish(m));
    }
}

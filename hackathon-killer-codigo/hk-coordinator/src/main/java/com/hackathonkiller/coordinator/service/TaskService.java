package com.hackathonkiller.coordinator.service;

import com.hackathonkiller.core.exception.InvalidTransitionException;
import com.hackathonkiller.core.message.MessageType;
import com.hackathonkiller.core.model.Agent;
import com.hackathonkiller.core.model.AgentStatus;
import com.hackathonkiller.core.model.Evidence;
import com.hackathonkiller.core.model.Task;
import com.hackathonkiller.core.state.TaskState;
import com.hackathonkiller.coordinator.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class TaskService {

    private static final EnumSet<TaskState> ACTIVE = EnumSet.of(TaskState.ASSIGNED, TaskState.IN_PROGRESS);

    private final TaskRepository repository;
    private final AgentService agents;
    private final MessageService messages;
    private final int maxActiveTasks;

    public TaskService(TaskRepository repository,
                       AgentService agents,
                       MessageService messages,
                       @Value("${hk.agent.max-active-tasks:2}") int maxActiveTasks) {
        this.repository = repository;
        this.agents = agents;
        this.messages = messages;
        this.maxActiveTasks = maxActiveTasks;
    }

    public synchronized Task create(String title, String description, List<String> acceptanceCriteria) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title es obligatorio");
        }
        String id = String.format("HK-%03d", repository.nextSequence());
        Task task = new Task(id, title, description, acceptanceCriteria);
        Task saved = repository.save(task);
        messages.emit(MessageType.TASK_CREATED, null, id, Map.of("title", title));
        return saved;
    }

    public synchronized Task markReady(String taskId) {
        Task task = get(taskId);
        task.transitionTo(TaskState.READY);
        return repository.save(task);
    }

    /** Asigna la tarea a un agente conectado, evitando sobrecarga y trabajo duplicado. */
    public synchronized Task assign(String taskId, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId es obligatorio");
        }
        Task task = get(taskId);
        Agent agent = agents.get(agentId);
        if (agent.getStatus() == AgentStatus.OFFLINE) {
            throw new InvalidTransitionException("El agente " + agentId + " está OFFLINE");
        }
        long active = repository.findAll().stream()
                .filter(t -> agentId.equals(t.getAssigneeId()) && ACTIVE.contains(t.getState()))
                .count();
        if (active >= maxActiveTasks) {
            throw new InvalidTransitionException(
                    "El agente " + agentId + " ya tiene " + active + " tareas activas (máx. " + maxActiveTasks + ")");
        }
        task.assignTo(agentId);
        repository.save(task);
        messages.emit(MessageType.TASK_ASSIGNED, agentId, taskId, Map.of("assignee", agentId));
        return task;
    }

    /** Cambio de estado genérico; el reviewer es obligatorio al pasar a VALIDATING. */
    public synchronized Task transition(String taskId, TaskState next, String reviewerId) {
        Task task = get(taskId);
        if (next == TaskState.VALIDATING) {
            if (reviewerId == null || reviewerId.isBlank()) {
                throw new InvalidTransitionException("VALIDATING requiere reviewerId");
            }
            if (reviewerId.equals(task.getAssigneeId())) {
                throw new InvalidTransitionException("El revisor debe ser distinto del autor (revisión independiente)");
            }
            agents.get(reviewerId);
            task.setReviewerId(reviewerId);
        }
        task.transitionTo(next);
        repository.save(task);
        messages.emit(messageTypeFor(next), task.getAssigneeId(), taskId, Map.of("state", next.name()));
        return task;
    }

    public synchronized Task addEvidence(String taskId, String type, String summary, boolean passed) {
        Task task = get(taskId);
        task.addEvidence(Evidence.of(type, summary, passed));
        return repository.save(task);
    }

    public synchronized Task setGitInfo(String taskId, String branch, String commitSha, String prUrl) {
        Task task = get(taskId);
        if (branch != null) task.setBranch(branch);
        if (commitSha != null) task.setCommitSha(commitSha);
        if (prUrl != null) task.setPrUrl(prUrl);
        repository.save(task);
        if (prUrl != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("pr_url", prUrl);
            messages.emit(MessageType.PR_CREATED, null, taskId, payload);
        }
        return task;
    }

    public Task get(String taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Tarea no encontrada: " + taskId));
    }

    public List<Task> list(String assigneeId) {
        return repository.findAll().stream()
                .filter(t -> assigneeId == null || assigneeId.equals(t.getAssigneeId()))
                .toList();
    }

    private static MessageType messageTypeFor(TaskState state) {
        return switch (state) {
            case IN_PROGRESS -> MessageType.TASK_ACCEPTED;
            case BLOCKED -> MessageType.TASK_BLOCKED;
            case READY_FOR_REVIEW -> MessageType.REVIEW_REQUESTED;
            case VALIDATING -> MessageType.TEST_STARTED;
            case PASSED -> MessageType.REVIEW_APPROVED;
            case FAILED, CHANGES_REQUESTED -> MessageType.REVIEW_REJECTED;
            case DONE -> MessageType.TASK_COMPLETED;
            case CANCELLED -> MessageType.TASK_CANCELLED;
            default -> MessageType.TASK_PROGRESS;
        };
    }
}

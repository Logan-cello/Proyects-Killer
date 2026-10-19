package com.hackathonkiller.core.model;

import com.hackathonkiller.core.exception.InvalidTransitionException;
import com.hackathonkiller.core.state.TaskState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Tarea con criterios de aceptación, evidencia y reglas de transición. */
public class Task {

    public static final int MAX_RETRIES = 3;
    public static final int MAX_REVIEW_ROUNDS = 3;

    private String id;
    private String title;
    private String description;
    private List<String> acceptanceCriteria = new ArrayList<>();
    private TaskState state = TaskState.BACKLOG;
    private String assigneeId;
    private String reviewerId;
    private String branch;
    private String commitSha;
    private String prUrl;
    private List<Evidence> evidence = new ArrayList<>();
    private int retries;
    private int reviewRounds;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Task() {
        // requerido por Jackson
    }

    public Task(String id, String title, String description, List<String> acceptanceCriteria) {
        if (acceptanceCriteria == null || acceptanceCriteria.isEmpty()) {
            throw new IllegalArgumentException("Una tarea debe tener criterios de aceptación");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.acceptanceCriteria = new ArrayList<>(acceptanceCriteria);
    }

    /** Cambia de estado aplicando la máquina de estados y las reglas de negocio. */
    public void transitionTo(TaskState next) {
        if (!state.canTransitionTo(next)) {
            throw new InvalidTransitionException(
                    "Transición inválida en " + id + ": " + state + " -> " + next);
        }
        switch (next) {
            case ASSIGNED -> require(assigneeId != null, "ASSIGNED requiere un responsable");
            case READY_FOR_REVIEW -> require(!evidence.isEmpty(),
                    "READY_FOR_REVIEW requiere evidencia (commit, tests...)");
            case VALIDATING -> require(reviewerId != null, "VALIDATING requiere un revisor asignado");
            case PASSED -> require(hasPassingTestEvidence(),
                    "Una aprobación sin pruebas en verde no es válida");
            case IN_PROGRESS -> {
                if (state == TaskState.FAILED || state == TaskState.CHANGES_REQUESTED) {
                    require(retries < MAX_RETRIES, "Límite de reintentos alcanzado (" + MAX_RETRIES + ")");
                    retries++;
                }
            }
            default -> { }
        }
        if (next == TaskState.VALIDATING) {
            require(reviewRounds < MAX_REVIEW_ROUNDS, "Límite de rondas de revisión alcanzado");
            reviewRounds++;
        }
        if (next == TaskState.READY && state == TaskState.ASSIGNED) {
            assigneeId = null; // la tarea fue rechazada, vuelve a la cola
        }
        this.state = next;
        this.updatedAt = Instant.now();
    }

    public void assignTo(String agentId) {
        if (!state.canTransitionTo(TaskState.ASSIGNED)) {
            throw new InvalidTransitionException(
                    "Transición inválida en " + id + ": " + state + " -> " + TaskState.ASSIGNED);
        }
        require(agentId != null && !agentId.isBlank(), "ASSIGNED requiere un responsable");
        this.assigneeId = agentId;
        transitionTo(TaskState.ASSIGNED);
    }

    public void addEvidence(Evidence e) {
        this.evidence.add(e);
        this.updatedAt = Instant.now();
    }

    public boolean hasPassingTestEvidence() {
        return evidence.stream()
                .filter(e -> "TEST_RESULT".equals(e.type()))
                .reduce((first, last) -> last)
                .map(Evidence::passed)
                .orElse(false);
    }

    public Task copy() {
        Task copy = new Task();
        copy.id = id;
        copy.title = title;
        copy.description = description;
        copy.acceptanceCriteria = new ArrayList<>(acceptanceCriteria);
        copy.state = state;
        copy.assigneeId = assigneeId;
        copy.reviewerId = reviewerId;
        copy.branch = branch;
        copy.commitSha = commitSha;
        copy.prUrl = prUrl;
        copy.evidence = new ArrayList<>(evidence);
        copy.retries = retries;
        copy.reviewRounds = reviewRounds;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidTransitionException(message);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> c) { this.acceptanceCriteria = c; }
    public TaskState getState() { return state; }
    public void setState(TaskState state) { this.state = state; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public List<Evidence> getEvidence() { return evidence; }
    public void setEvidence(List<Evidence> evidence) { this.evidence = evidence; }
    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
    public int getReviewRounds() { return reviewRounds; }
    public void setReviewRounds(int reviewRounds) { this.reviewRounds = reviewRounds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

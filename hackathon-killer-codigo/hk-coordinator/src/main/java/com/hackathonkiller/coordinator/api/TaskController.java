package com.hackathonkiller.coordinator.api;

import com.hackathonkiller.core.model.Task;
import com.hackathonkiller.core.state.TaskState;
import com.hackathonkiller.coordinator.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    public record CreateRequest(String title, String description, List<String> acceptanceCriteria) { }

    public record AssignRequest(String agentId) { }

    public record TransitionRequest(TaskState state, String reviewerId) { }

    public record EvidenceRequest(String type, String summary, boolean passed) { }

    public record GitRequest(String branch, String commitSha, String prUrl) { }

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    public Task create(@RequestBody CreateRequest req) {
        return service.create(req.title(), req.description(), req.acceptanceCriteria());
    }

    @GetMapping
    public List<Task> list(@RequestParam(name = "assignee", required = false) String assignee) {
        return service.list(assignee);
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/ready")
    public Task ready(@PathVariable("id") String id) {
        return service.markReady(id);
    }

    @PostMapping("/{id}/assign")
    public Task assign(@PathVariable("id") String id, @RequestBody AssignRequest req) {
        return service.assign(id, req.agentId());
    }

    @PostMapping("/{id}/transition")
    public Task transition(@PathVariable("id") String id, @RequestBody TransitionRequest req) {
        if (req.state() == null) {
            throw new IllegalArgumentException("state es obligatorio");
        }
        return service.transition(id, req.state(), req.reviewerId());
    }

    @PostMapping("/{id}/evidence")
    public Task evidence(@PathVariable("id") String id, @RequestBody EvidenceRequest req) {
        return service.addEvidence(id, req.type(), req.summary(), req.passed());
    }

    @PostMapping("/{id}/git")
    public Task git(@PathVariable("id") String id, @RequestBody GitRequest req) {
        return service.setGitInfo(id, req.branch(), req.commitSha(), req.prUrl());
    }
}

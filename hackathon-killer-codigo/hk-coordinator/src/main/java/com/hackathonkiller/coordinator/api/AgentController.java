package com.hackathonkiller.coordinator.api;

import com.hackathonkiller.core.model.AdapterType;
import com.hackathonkiller.core.model.Agent;
import com.hackathonkiller.core.model.AgentRole;
import com.hackathonkiller.coordinator.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    public record RegisterRequest(String name, AgentRole role, AdapterType adapterType, List<String> capabilities) { }

    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    @PostMapping
    public Agent register(@RequestBody RegisterRequest req) {
        if (req.role() == null || req.adapterType() == null) {
            throw new IllegalArgumentException("role y adapterType son obligatorios");
        }
        return service.register(req.name(), req.role(), req.adapterType(), req.capabilities());
    }

    @GetMapping
    public List<Agent> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Agent get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/heartbeat")
    public Agent heartbeat(@PathVariable("id") String id) {
        return service.heartbeat(id);
    }
}

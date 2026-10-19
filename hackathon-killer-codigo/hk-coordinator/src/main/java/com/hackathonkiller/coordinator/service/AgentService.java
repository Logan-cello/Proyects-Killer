package com.hackathonkiller.coordinator.service;

import com.hackathonkiller.core.message.MessageType;
import com.hackathonkiller.core.model.AdapterType;
import com.hackathonkiller.core.model.Agent;
import com.hackathonkiller.core.model.AgentRole;
import com.hackathonkiller.core.model.AgentStatus;
import com.hackathonkiller.coordinator.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AgentService {

    private final AgentRepository repository;
    private final MessageService messages;
    private final Duration heartbeatTimeout;

    public AgentService(AgentRepository repository,
                        MessageService messages,
                        @Value("${hk.heartbeat.timeout-seconds:30}") long timeoutSeconds) {
        this.repository = repository;
        this.messages = messages;
        this.heartbeatTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    /** Registra (o re-registra tras una desconexión) a un agente. El id sale del nombre. */
    public synchronized Agent register(String name, AgentRole role, AdapterType adapterType,
                                       List<String> capabilities) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name es obligatorio");
        }
        String id = toId(name);
        Agent agent = repository.findById(id).orElseGet(() -> new Agent(id, name, role, adapterType, capabilities));
        agent.setName(name);
        agent.setRole(role);
        agent.setAdapterType(adapterType);
        agent.setCapabilities(capabilities == null ? List.of() : capabilities);
        agent.setStatus(AgentStatus.ONLINE);
        agent.heartbeat();
        repository.save(agent);
        messages.emit(MessageType.AGENT_REGISTERED, null, null,
                Map.of("agent_id", id, "role", role.name(), "adapter", adapterType.name()));
        return agent;
    }

    public synchronized Agent heartbeat(String agentId) {
        Agent agent = get(agentId);
        agent.heartbeat();
        repository.save(agent);
        return agent;
    }

    public Agent get(String agentId) {
        return repository.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("Agente no encontrado: " + agentId));
    }

    public List<Agent> list() {
        return repository.findAll();
    }

    /** Detecta agentes desconectados cada 10 s. */
    @Scheduled(fixedDelay = 10_000)
    public synchronized void markOfflineAgents() {
        Instant limit = Instant.now().minus(heartbeatTimeout);
        for (Agent a : repository.findAll()) {
            if (a.getStatus() != AgentStatus.OFFLINE && a.getLastHeartbeat().isBefore(limit)) {
                a.setStatus(AgentStatus.OFFLINE);
                repository.save(a);
                messages.emit(MessageType.AGENT_HEARTBEAT, null, null,
                        Map.of("agent_id", a.getId(), "status", "OFFLINE"));
            }
        }
    }

    static String toId(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}

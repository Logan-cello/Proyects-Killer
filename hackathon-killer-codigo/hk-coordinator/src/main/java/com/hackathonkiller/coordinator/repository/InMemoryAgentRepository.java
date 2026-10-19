package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.model.Agent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria. Siguiente paso: reemplazar por SQLite (JDBC) manteniendo la interfaz. */
@Repository
public class InMemoryAgentRepository implements AgentRepository {

    private final Map<String, Agent> store = new ConcurrentHashMap<>();

    @Override
    public Agent save(Agent agent) {
        store.put(agent.getId(), agent);
        return agent;
    }

    @Override
    public Optional<Agent> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Agent> findAll() {
        return store.values().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
    }
}

package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.model.Agent;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    Agent save(Agent agent);

    Optional<Agent> findById(String id);

    List<Agent> findAll();
}

package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);

    Optional<Task> findById(String id);

    List<Task> findAll();

    /** Siguiente número secuencial para generar ids HK-001, HK-002... */
    int nextSequence();
}

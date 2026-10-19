package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.model.Task;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, Task> store = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Task save(Task task) {
        Task saved = task.copy();
        store.put(saved.getId(), saved);
        return saved.copy();
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(store.get(id)).map(Task::copy);
    }

    @Override
    public List<Task> findAll() {
        return store.values().stream()
                .map(Task::copy)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    @Override
    public int nextSequence() {
        return sequence.incrementAndGet();
    }
}

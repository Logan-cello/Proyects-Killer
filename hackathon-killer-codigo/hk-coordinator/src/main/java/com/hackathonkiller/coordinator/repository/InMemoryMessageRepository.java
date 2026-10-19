package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.message.Message;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class InMemoryMessageRepository implements MessageRepository {

    private final List<Message> log = new ArrayList<>();
    private final Set<String> ids = new HashSet<>();

    @Override
    public synchronized boolean saveIfAbsent(Message message) {
        if (!ids.add(message.messageId())) {
            return false;
        }
        log.add(message);
        return true;
    }

    @Override
    public synchronized List<Message> findAll() {
        return List.copyOf(log);
    }
}

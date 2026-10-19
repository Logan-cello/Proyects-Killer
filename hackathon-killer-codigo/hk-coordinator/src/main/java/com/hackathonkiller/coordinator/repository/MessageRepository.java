package com.hackathonkiller.coordinator.repository;

import com.hackathonkiller.core.message.Message;

import java.util.List;

public interface MessageRepository {
    /** @return true si se guardó; false si el message_id ya existía (duplicado). */
    boolean saveIfAbsent(Message message);

    List<Message> findAll();
}

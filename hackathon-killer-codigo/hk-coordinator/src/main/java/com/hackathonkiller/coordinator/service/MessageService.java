package com.hackathonkiller.coordinator.service;

import com.hackathonkiller.core.message.Message;
import com.hackathonkiller.core.message.MessageType;
import com.hackathonkiller.coordinator.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Registro de mensajes estructurados con deduplicación por message_id. */
@Service
public class MessageService {

    public static final String COORDINATOR = "coordinator";

    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    /** @return true si es nuevo, false si era un duplicado (se ignora). */
    public boolean publish(Message message) {
        if (message.isExpired()) {
            return false;
        }
        return repository.saveIfAbsent(message);
    }

    public void emit(MessageType type, String recipient, String taskId, Map<String, Object> payload) {
        publish(Message.simple(COORDINATOR, recipient, type, taskId, payload));
    }

    public List<Message> find(String taskId, String recipient) {
        return repository.findAll().stream()
                .filter(m -> taskId == null || taskId.equals(m.taskId()))
                .filter(m -> recipient == null || recipient.equals(m.recipient()))
                .toList();
    }
}

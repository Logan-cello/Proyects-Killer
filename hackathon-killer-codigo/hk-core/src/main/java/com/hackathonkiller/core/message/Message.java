package com.hackathonkiller.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Mensaje estructurado entre agentes. El JSON usa snake_case según el protocolo del README.
 */
public record Message(
        @JsonProperty("message_id") String messageId,
        @JsonProperty("conversation_id") String conversationId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("sender") String sender,
        @JsonProperty("recipient") String recipient,
        @JsonProperty("type") MessageType type,
        @JsonProperty("task_id") String taskId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("status") String status,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("payload") Map<String, Object> payload,
        @JsonProperty("requires_response") boolean requiresResponse,
        @JsonProperty("expires_at") Instant expiresAt) {

    public Message {
        if (messageId == null || messageId.isBlank()) {
            messageId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (priority == null) {
            priority = Priority.NORMAL;
        }
        if (payload == null) {
            payload = Map.of();
        }
        if (type == null) {
            throw new IllegalArgumentException("type es obligatorio");
        }
        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException("sender es obligatorio");
        }
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public static Message simple(String sender, String recipient, MessageType type, String taskId,
                                 Map<String, Object> payload) {
        return new Message(null, null, null, sender, recipient, type, taskId, null,
                "SENT", Priority.NORMAL, payload, false, null);
    }
}

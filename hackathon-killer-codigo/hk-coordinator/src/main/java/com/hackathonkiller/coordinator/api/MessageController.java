package com.hackathonkiller.coordinator.api;

import com.hackathonkiller.core.message.Message;
import com.hackathonkiller.coordinator.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    /** 201 si es nuevo, 200 si el message_id ya existía (duplicado ignorado). */
    @PostMapping
    public ResponseEntity<Message> send(@RequestBody Message message) {
        boolean created = service.publish(message);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).body(message);
    }

    @GetMapping
    public List<Message> list(@RequestParam(name = "taskId", required = false) String taskId,
                              @RequestParam(name = "recipient", required = false) String recipient) {
        return service.find(taskId, recipient);
    }
}

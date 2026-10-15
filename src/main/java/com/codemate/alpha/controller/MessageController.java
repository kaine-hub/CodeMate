package com.codemate.alpha.controller;

import com.codemate.alpha.entity.*;
import com.codemate.alpha.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final int MAX_LENGTH = 2000;

    private final MessageRepository messageRepository;
    private final ApplyToProjectRepository applicationRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository,
            ApplyToProjectRepository applicationRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    // participants of a thread: the applicant, the project poster, the team leader
    private boolean isParticipant(ApplyToProject app, Long userId) {
        Project p = app.getProject();
        return app.getUser().getUserId().equals(userId)
                || p.getPostedByUser().getUserId().equals(userId)
                || p.getTeam().getLeader().getUserId().equals(userId);
    }

    // GET THREAD: GET /api/messages/application/5?userId=2
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<?> getThread(@PathVariable Long applicationId, @RequestParam Long userId) {
        ApplyToProject app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Application not found");
        }
        if (!isParticipant(app, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not part of this conversation");
        }
        return ResponseEntity.ok(messageRepository.findByApplication_ApplicationIdOrderBySentAtAsc(applicationId));
    }

    // SEND: body { "application": {"applicationId": 5}, "sender": {"userId": 2},
    // "messageText": "Hi" }
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        if (message.getApplication() == null || message.getApplication().getApplicationId() == null) {
            return ResponseEntity.badRequest().body("application.applicationId is required");
        }
        if (message.getSender() == null || message.getSender().getUserId() == null) {
            return ResponseEntity.badRequest().body("sender.userId is required");
        }
        String text = message.getMessageText() == null ? "" : message.getMessageText().trim();
        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body("Message text is required");
        }
        if (text.length() > MAX_LENGTH) {
            return ResponseEntity.badRequest().body("Message must be at most " + MAX_LENGTH + " characters");
        }

        ApplyToProject app = applicationRepository.findById(message.getApplication().getApplicationId()).orElse(null);
        if (app == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Application not found");
        }
        User sender = userRepository.findById(message.getSender().getUserId()).orElse(null);
        if (sender == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sender not found");
        }
        if (!isParticipant(app, sender.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not part of this conversation");
        }
        if ("REJECTED".equals(app.getApplicationStatus())) {
            return ResponseEntity.badRequest().body("This application was rejected, the conversation is closed");
        }

        // build a fresh entity so a client can't inject messageId / sentAt
        Message toSave = new Message();
        toSave.setApplication(app);
        toSave.setSender(sender);
        toSave.setMessageText(text);
        toSave.setSentAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(messageRepository.save(toSave));
    }

    // DELETE OWN MESSAGE: DELETE /api/messages/9?userId=2
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, @RequestParam Long userId) {
        Message m = messageRepository.findById(id).orElse(null);
        if (m == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Message not found");
        }
        if (!m.getSender().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own messages");
        }
        messageRepository.delete(m);
        return ResponseEntity.noContent().build();
    }
}
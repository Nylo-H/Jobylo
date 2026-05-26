package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.Message.ConversationResponse;
import com.example.TestAPI.DTO.Message.MessageResponse;
import com.example.TestAPI.DTO.Message.SendMessageRequest;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.Message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/start/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> startConversation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID jobId,
            @RequestBody Map<String, String> body) {

        String content = body.getOrDefault("content", "");
        MessageResponse response = messageService.startConversation(currentUser, jobId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SendMessageRequest request) {

        MessageResponse response = messageService.sendMessage(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversation/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MessageResponse>> getMessagesByConversation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<MessageResponse> messages = messageService.getMessagesByConversation(currentUser, conversationId, page, size);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/{messageId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID messageId) {

        messageService.markAsRead(currentUser, messageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/conversation/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> markAllConversationAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID conversationId) {

        Map<String, Object> result = messageService.markAllConversationAsRead(currentUser, conversationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal User currentUser) {

        List<ConversationResponse> conversations = messageService.getConversations(currentUser);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {

        long count = messageService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}

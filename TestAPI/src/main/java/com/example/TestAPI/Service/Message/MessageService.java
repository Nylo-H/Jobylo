package com.example.TestAPI.Service.Message;

import com.example.TestAPI.DTO.Message.ConversationResponse;
import com.example.TestAPI.DTO.Message.MessageResponse;
import com.example.TestAPI.DTO.Message.SendMessageRequest;
import com.example.TestAPI.Model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MessageService {
    MessageResponse startConversation(User sender, UUID jobId, String content);
    MessageResponse sendMessage(User sender, SendMessageRequest request);
    Page<MessageResponse> getMessagesByConversation(User currentUser, UUID conversationId, int page, int size);
    void markAsRead(User currentUser, UUID messageId);
    Map<String, Object> markAllConversationAsRead(User currentUser, UUID conversationId);
    List<ConversationResponse> getConversations(User currentUser);
    long getUnreadCount(User currentUser);
}

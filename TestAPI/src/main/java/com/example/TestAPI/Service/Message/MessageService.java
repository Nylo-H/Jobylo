package com.example.TestAPI.Service.Message;

import com.example.TestAPI.DTO.Message.ConversationResponse;
import com.example.TestAPI.DTO.Message.MessageResponse;
import com.example.TestAPI.DTO.Message.SendMessageRequest;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageResponse startConversation(User sender, UUID jobId, String content);
    MessageResponse sendMessage(User sender, SendMessageRequest request);
    List<MessageResponse> getMessagesByConversation(User currentUser, UUID conversationId);
    void markAsRead(User currentUser, UUID messageId);
    List<ConversationResponse> getConversations(User currentUser);
    long getUnreadCount(User currentUser);
}

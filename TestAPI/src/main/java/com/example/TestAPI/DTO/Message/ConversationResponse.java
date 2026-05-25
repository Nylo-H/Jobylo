package com.example.TestAPI.DTO.Message;

import java.util.Date;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID jobId,
        String jobTitle,
        UUID otherUserId,
        String otherUserUsername,
        String lastMessage,
        Date lastMessageTimestamp,
        long unreadCount
) {}

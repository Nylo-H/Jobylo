package com.example.TestAPI.DTO.Message;

import java.util.Date;
import java.util.UUID;

public record NotificationEvent(
        String type,
        UUID conversationId,
        UUID jobId,
        String jobTitle,
        UUID senderId,
        String senderUsername,
        UUID receiverId,
        String lastMessage,
        Date lastMessageTimestamp,
        long unreadCount
) { }

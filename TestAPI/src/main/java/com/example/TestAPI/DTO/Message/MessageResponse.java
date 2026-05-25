package com.example.TestAPI.DTO.Message;

import java.util.Date;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderUsername,
        UUID receiverId,
        String receiverUsername,
        UUID jobId,
        String content,
        Date timestamp,
        boolean isRead
) {}

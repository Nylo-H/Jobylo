package com.example.TestAPI.DTO.Message;

import java.util.Date;
import java.util.UUID;

public record ReadReceiptEvent(
        UUID conversationId,
        UUID readByUserId,
        String readByUsername,
        Date readAt
) { }

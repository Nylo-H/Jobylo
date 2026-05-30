package com.example.TestAPI.DTO.Audit;

import com.example.TestAPI.Model.Enum.ActionType;

import java.util.Date;
import java.util.UUID;

public record ActionLogResponse(
        UUID id,
        UUID userId,
        String username,
        ActionType action,
        String details,
        Date timestamp
) {}

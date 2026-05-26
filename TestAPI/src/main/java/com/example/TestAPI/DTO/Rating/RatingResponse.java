package com.example.TestAPI.DTO.Rating;

import com.example.TestAPI.Model.Enum.RatingTarget;

import java.util.Date;
import java.util.UUID;

public record RatingResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID raterId,
        String raterUsername,
        UUID targetId,
        String targetUsername,
        RatingTarget targetType,
        int score,
        String comment,
        Date createdAt
) { }

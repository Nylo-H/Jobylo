package com.example.TestAPI.DTO.Rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RatingRequest(
        @NotNull(message = "L'ID du job est obligatoire")
        UUID jobId,

        @NotNull(message = "L'ID de l'utilisateur noté est obligatoire")
        UUID targetUserId,

        @Min(value = 1, message = "La note minimale est 1")
        @Max(value = 5, message = "La note maximale est 5")
        int score,

        String comment
) { }

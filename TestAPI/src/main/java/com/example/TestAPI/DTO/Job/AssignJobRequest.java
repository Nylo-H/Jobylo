package com.example.TestAPI.DTO.Job;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignJobRequest(
        @NotNull(message = "L'ID du worker est obligatoire")
        UUID workerId
) {
}

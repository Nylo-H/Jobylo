package com.example.TestAPI.DTO.Job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateJobRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String title,

        String description,

        String location,

        @NotNull(message = "Le prix est obligatoire")
        @Positive(message = "Le prix doit être positif")
        BigDecimal price,

        List<String> images
) { }

package com.example.TestAPI.DTO.Kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KycSubmissionRequest(
        @NotBlank String fileUrl,
        @NotNull String documentType
) {}

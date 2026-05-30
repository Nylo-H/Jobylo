package com.example.TestAPI.DTO.Admin;

import com.example.TestAPI.Model.Enum.KycStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKycRequest(
        @NotNull KycStatus status,
        String rejectionReason
) {}

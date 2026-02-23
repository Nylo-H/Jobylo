package com.example.TestAPI.DTO.Kyc;

import java.util.UUID;

public record KycResponse(
        UUID id,
        UUID userId,
        String status, // PENDING, APPROVED, REJECTED
        String verificationDocument
) {
}

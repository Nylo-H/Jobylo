package com.example.TestAPI.DTO.Kyc;

import java.util.Date;
import java.util.UUID;

public record KycDocumentResponse(
        UUID id,
        UUID userId,
        String fileUrl,
        String documentType,
        String status,
        UUID verifiedById,
        Date submittedAt,
        String rejectionReason
) {}

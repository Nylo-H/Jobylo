package com.example.TestAPI.DTO.Payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID buyerId,
        String buyerUsername,
        UUID sellerId,
        String sellerUsername,
        BigDecimal amount,
        BigDecimal commissionPercentage,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        String status,
        String paymentMethod,
        Date createdAt,
        Date updatedAt
) {}

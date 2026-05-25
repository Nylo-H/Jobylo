package com.example.TestAPI.DTO.Payment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmDeliveryRequest(
        @NotNull UUID transactionId
) {}

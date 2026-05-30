package com.example.TestAPI.DTO.Application;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplyRequest(
        @NotNull UUID jobId,
        String coverLetter
) { }

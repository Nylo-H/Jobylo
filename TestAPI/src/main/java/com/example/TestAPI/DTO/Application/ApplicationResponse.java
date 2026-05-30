package com.example.TestAPI.DTO.Application;

import com.example.TestAPI.Model.Enum.ApplicationStatus;

import java.util.Date;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID workerId,
        String workerUsername,
        String coverLetter,
        ApplicationStatus status,
        Date createdAt
) { }

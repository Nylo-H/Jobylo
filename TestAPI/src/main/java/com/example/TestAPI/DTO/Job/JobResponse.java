package com.example.TestAPI.DTO.Job;

import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String description,
        UUID creatorId,
        UUID assigneeId,
        String status
) {

}

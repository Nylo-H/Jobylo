package com.example.TestAPI.DTO.User;

public record UserStatsResponse(
        long totalJobsCreated,
        long totalJobsInProgress,
        long totalJobsCompleted,
        Double averageRating,
        int totalRatings,
        long totalApplicationsReceived,
        long totalApplicationsSent
) { }

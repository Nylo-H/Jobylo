package com.example.TestAPI.Service.Rating;

import com.example.TestAPI.DTO.Rating.RatingRequest;
import com.example.TestAPI.DTO.Rating.RatingResponse;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface RatingService {
    RatingResponse submitRating(User rater, RatingRequest request);
    List<RatingResponse> getRatingsByUser(UUID userId);
    List<RatingResponse> getRatingsByJob(UUID jobId);
    List<RatingResponse> getMyRatings(User currentUser);
}

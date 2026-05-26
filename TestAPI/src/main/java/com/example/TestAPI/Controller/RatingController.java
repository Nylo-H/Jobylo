package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.Rating.RatingRequest;
import com.example.TestAPI.DTO.Rating.RatingResponse;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.Rating.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponse> submitRating(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RatingRequest request) {

        RatingResponse response = ratingService.submitRating(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ratingService.getRatingsByUser(userId));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RatingResponse>> getRatingsByJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ratingService.getRatingsByJob(jobId));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RatingResponse>> getMyRatings(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ratingService.getMyRatings(currentUser));
    }
}

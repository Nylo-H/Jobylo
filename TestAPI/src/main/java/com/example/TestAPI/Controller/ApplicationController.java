package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.Application.ApplicationResponse;
import com.example.TestAPI.DTO.Application.ApplyRequest;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.Application.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> apply(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID jobId,
            @RequestBody(required = false) Map<String, String> body) {

        String coverLetter = body != null ? body.getOrDefault("coverLetter", null) : null;
        ApplicationResponse response = applicationService.apply(currentUser, jobId, coverLetter);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/jobs/{jobId}/applicants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationResponse>> getApplicants(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID jobId) {

        List<ApplicationResponse> applicants = applicationService.getApplicants(currentUser, jobId);
        return ResponseEntity.ok(applicants);
    }

    @PostMapping("/jobs/{jobId}/reject/{workerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> rejectApplicant(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID jobId,
            @PathVariable UUID workerId) {

        applicationService.rejectApplicant(currentUser, jobId, workerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal User currentUser) {

        List<ApplicationResponse> applications = applicationService.getMyApplications(currentUser);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/jobs/{jobId}/applicants/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> countApplicants(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID jobId) {

        long count = applicationService.countByJob(jobId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

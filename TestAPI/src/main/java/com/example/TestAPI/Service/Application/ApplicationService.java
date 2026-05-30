package com.example.TestAPI.Service.Application;

import com.example.TestAPI.DTO.Application.ApplicationResponse;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {
    ApplicationResponse apply(User worker, UUID jobId, String coverLetter);
    List<ApplicationResponse> getApplicants(User currentUser, UUID jobId);
    void rejectApplicant(User currentUser, UUID jobId, UUID workerId);
    List<ApplicationResponse> getMyApplications(User worker);
    long countByJob(UUID jobId);
}

package com.example.TestAPI.Service.JobOffer;

import com.example.TestAPI.DTO.Job.JobResponse;
import com.example.TestAPI.Model.JobOffer;

import java.util.List;
import java.util.UUID;

public interface JobService {
    JobResponse createJob(JobOffer job);
    JobResponse getJob(UUID id);
    List<JobResponse> getJobsByCreator(UUID creatorId);
    List<JobResponse> getJobsByAssignee(UUID assigneeId);
    JobResponse updateJob(UUID id, JobOffer job);
    void deleteJob(UUID id);
}

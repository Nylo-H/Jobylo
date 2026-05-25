package com.example.TestAPI.Service.JobOffer;

import com.example.TestAPI.DTO.Job.AssignJobRequest;
import com.example.TestAPI.DTO.Job.CreateJobRequest;
import com.example.TestAPI.DTO.Job.UpdateJobRequest;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.Enum.JobStatus;
import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.JobOfferRepository;
import com.example.TestAPI.Repository.UserRepository;
import com.example.TestAPI.Service.Audit.AuditService;
import com.example.TestAPI.Service.Audit.KycGuard;
import com.example.TestAPI.exception.BusinessException;
import com.example.TestAPI.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService{

    private final JobOfferRepository jobRepository;
    private final UserRepository userRepository;
    private final KycGuard kycGuard;
    private final AuditService auditService;

    @Override
    public JobOffer createJob(User creator, CreateJobRequest request) {
        kycGuard.requireVerified(creator);

        JobOffer job = JobOffer.builder()
                .title(request.title())
                .description(request.description())
                .location(request.location())
                .price(request.price())
                .creator(creator)
                .status(JobStatus.PENDING)
                .createdAt(new Date())
                .updatedAt(new Date())
                .images(request.images() != null ? new java.util.ArrayList<>(request.images()) : new java.util.ArrayList<>())
                .build();

        job = jobRepository.save(job);
        auditService.log(creator, ActionType.CREATE_JOB, "Job: " + job.getId());
        return job;
    }

    @Override
    public JobOffer updateJob(UUID jobId, User currentUser, UpdateJobRequest request) {
        kycGuard.requireVerified(currentUser);

        JobOffer job = getJobById(jobId);

        if (!job.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous n'êtes pas autorisé à modifier cette annonce", ErrorCode.FORBIDDEN);
        }

        if (job.getStatus() != JobStatus.PENDING) {
            throw new BusinessException("Impossible de modifier une annonce déjà attribuée ou en cours", ErrorCode.BAD_REQUEST);
        }

        if (request.title() != null) job.setTitle(request.title());
        if (request.description() != null) job.setDescription(request.description());
        if (request.location() != null) job.setLocation(request.location());
        if (request.price() != null) job.setPrice(request.price());
        if (request.images() != null) job.setImages(new java.util.ArrayList<>(request.images()));

        job.setUpdatedAt(new Date());
        job = jobRepository.save(job);
        auditService.log(currentUser, ActionType.UPDATE_JOB, "Job: " + jobId);
        return job;
    }

    @Override
    public JobOffer assignJob(UUID jobId, User currentUser, AssignJobRequest request) {
        kycGuard.requireVerified(currentUser);

        JobOffer job = getJobById(jobId);

        if (!job.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous n'êtes pas autorisé à attribuer cette annonce", ErrorCode.FORBIDDEN);
        }

        if (job.getStatus() != JobStatus.PENDING) {
            throw new BusinessException("Cette annonce n'est plus disponible", ErrorCode.BAD_REQUEST);
        }

        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", ErrorCode.NOT_FOUND));

        if (worker.getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous ne pouvez pas attribuer une annonce à vous-même", ErrorCode.BAD_REQUEST);
        }

        kycGuard.requireVerified(worker);

        job.setWorker(worker);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUpdatedAt(new Date());

        job = jobRepository.save(job);
        auditService.log(currentUser, ActionType.ASSIGN_JOB, "Job: " + jobId + " -> Worker: " + worker.getId());
        return job;
    }

    @Override
    public JobOffer updateJobStatus(UUID jobId, User currentUser, JobStatus status) {
        JobOffer job = getJobById(jobId);

        boolean isCreator = job.getCreator().getId().equals(currentUser.getId());
        boolean isWorker = job.getWorker() != null && job.getWorker().getId().equals(currentUser.getId());

        if (!isCreator && !isWorker) {
            throw new BusinessException("Vous n'êtes pas autorisé à modifier le statut", ErrorCode.FORBIDDEN);
        }

        validateStatusTransition(job.getStatus(), status, isCreator, isWorker);

        job.setStatus(status);
        job.setUpdatedAt(new Date());

        if (status == JobStatus.DONE) {
            job.setWorker(null);
            auditService.log(currentUser, ActionType.COMPLETE_JOB, "Job: " + jobId);
        }

        return jobRepository.save(job);
    }

    @Override
    public JobOffer getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("Annonce non trouvée", ErrorCode.NOT_FOUND));
    }

    @Override
    public List<JobOffer> getJobsCreatedByUser(User user) {
        return jobRepository.findByCreatorOrderByCreatedAtDesc(user);
    }

    @Override
    public List<JobOffer> getJobsAssignedToUser(User user) {
        return jobRepository.findByWorkerOrderByCreatedAtDesc(user);
    }

    @Override
    public List<JobOffer> getAvailableJobs() {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.PENDING);
    }

    @Override
    public void deleteJob(UUID jobId, User currentUser) {
        kycGuard.requireVerified(currentUser);

        JobOffer job = getJobById(jobId);

        if (!job.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous n'êtes pas autorisé à supprimer cette annonce", ErrorCode.FORBIDDEN);
        }

        if (job.getStatus() == JobStatus.IN_PROGRESS) {
            throw new BusinessException("Impossible de supprimer une annonce en cours", ErrorCode.BAD_REQUEST);
        }

        jobRepository.delete(job);
        auditService.log(currentUser, ActionType.DELETE_JOB, "Job: " + jobId);
    }

    @Override
    @Transactional
    public JobOffer addImages(UUID jobId, User currentUser, List<String> imageUrls) {
        kycGuard.requireVerified(currentUser);

        JobOffer job = getJobById(jobId);

        if (!job.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous n'êtes pas autorisé à modifier cette annonce", ErrorCode.FORBIDDEN);
        }

        if (job.getImages() == null) {
            job.setImages(new java.util.ArrayList<>());
        }
        job.getImages().addAll(imageUrls);
        job.setUpdatedAt(new Date());
        job = jobRepository.save(job);
        auditService.log(currentUser, ActionType.UPDATE_JOB, "Images ajoutées au Job: " + jobId);
        return job;
    }

    @Override
    @Transactional
    public JobOffer removeImage(UUID jobId, User currentUser, String imageUrl) {
        kycGuard.requireVerified(currentUser);

        JobOffer job = getJobById(jobId);

        if (!job.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException("Vous n'êtes pas autorisé à modifier cette annonce", ErrorCode.FORBIDDEN);
        }

        if (job.getImages() != null) {
            job.getImages().remove(imageUrl);
        }
        job.setUpdatedAt(new Date());
        job = jobRepository.save(job);
        auditService.log(currentUser, ActionType.UPDATE_JOB, "Image supprimée du Job: " + jobId);
        return job;
    }

    private void validateStatusTransition(JobStatus currentStatus, JobStatus newStatus, boolean isCreator, boolean isWorker) {
        if (isCreator && newStatus == JobStatus.DONE && currentStatus == JobStatus.IN_PROGRESS) {
            return;
        }
        if (isWorker && newStatus == JobStatus.DONE && currentStatus == JobStatus.IN_PROGRESS) {
            return;
        }
        if (isCreator && newStatus == JobStatus.PENDING && currentStatus == JobStatus.PENDING) {
            return;
        }
        throw new BusinessException("Transition de statut invalide", ErrorCode.BAD_REQUEST);
    }
}

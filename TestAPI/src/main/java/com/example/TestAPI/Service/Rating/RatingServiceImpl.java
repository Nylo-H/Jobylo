package com.example.TestAPI.Service.Rating;

import com.example.TestAPI.DTO.Rating.RatingRequest;
import com.example.TestAPI.DTO.Rating.RatingResponse;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.Enum.RatingTarget;
import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.Rating;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Model.Enum.JobStatus;
import com.example.TestAPI.Repository.JobOfferRepository;
import com.example.TestAPI.Repository.RatingRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final JobOfferRepository jobRepository;
    private final UserRepository userRepository;
    private final KycGuard kycGuard;
    private final AuditService auditService;

    @Override
    public RatingResponse submitRating(User rater, RatingRequest request) {
        kycGuard.requireVerified(rater);

        JobOffer job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new BusinessException("Job non trouvé", ErrorCode.NOT_FOUND));

        if (job.getStatus() != JobStatus.DONE) {
            throw new BusinessException("Impossible de noter un job qui n'est pas terminé", ErrorCode.BAD_REQUEST);
        }

        boolean isCreator = job.getCreator().getId().equals(rater.getId());
        boolean isWorker = job.getWorker() != null && job.getWorker().getId().equals(rater.getId());

        if (!isCreator && !isWorker) {
            throw new BusinessException("Vous n'êtes pas participant à ce job", ErrorCode.FORBIDDEN);
        }

        User target = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", ErrorCode.NOT_FOUND));

        boolean targetIsCreator = job.getCreator().getId().equals(target.getId());
        boolean targetIsWorker = job.getWorker() != null && job.getWorker().getId().equals(target.getId());

        if (!targetIsCreator && !targetIsWorker) {
            throw new BusinessException("L'utilisateur noté n'est pas participant à ce job", ErrorCode.BAD_REQUEST);
        }

        if (target.getId().equals(rater.getId())) {
            throw new BusinessException("Vous ne pouvez pas vous noter vous-même", ErrorCode.BAD_REQUEST);
        }

        if (ratingRepository.existsByRaterAndJob(rater, job)) {
            throw new BusinessException("Vous avez déjà noté ce job", ErrorCode.BAD_REQUEST);
        }

        RatingTarget targetType = targetIsCreator ? RatingTarget.CREATOR : RatingTarget.WORKER;

        Rating rating = Rating.builder()
                .rater(rater)
                .target(target)
                .job(job)
                .targetType(targetType)
                .score(request.score())
                .comment(request.comment())
                .createdAt(new Date())
                .build();

        rating = ratingRepository.save(rating);

        recalculateAverageRating(target);

        auditService.log(rater, ActionType.SUBMIT_RATING,
                "Note: " + request.score() + "/5 pour " + target.getUsername() + " sur Job: " + job.getId());

        return toResponse(rating);
    }

    @Override
    public List<RatingResponse> getRatingsByUser(UUID userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", ErrorCode.NOT_FOUND));

        return ratingRepository.findByTargetOrderByCreatedAtDesc(target)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResponse> getRatingsByJob(UUID jobId) {
        JobOffer job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("Job non trouvé", ErrorCode.NOT_FOUND));

        return ratingRepository.findByJobOrderByCreatedAtDesc(job)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResponse> getMyRatings(User currentUser) {
        return ratingRepository.findByTargetOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void recalculateAverageRating(User target) {
        List<Rating> ratings = ratingRepository.findByTargetOrderByCreatedAtDesc(target);
        double avg = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
        target.setAverageRating(Math.round(avg * 100.0) / 100.0);
        target.setTotalRatings(ratings.size());
        userRepository.save(target);
    }

    private RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getJob().getId(),
                rating.getJob().getTitle(),
                rating.getRater().getId(),
                rating.getRater().getUsername(),
                rating.getTarget().getId(),
                rating.getTarget().getUsername(),
                rating.getTargetType(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }
}

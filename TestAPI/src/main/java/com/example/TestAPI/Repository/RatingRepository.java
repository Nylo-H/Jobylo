package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.Rating;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findByTargetOrderByCreatedAtDesc(User target);

    List<Rating> findByJobOrderByCreatedAtDesc(JobOffer job);

    Optional<Rating> findByRaterAndJob(User rater, JobOffer job);

    boolean existsByRaterAndJob(User rater, JobOffer job);
}

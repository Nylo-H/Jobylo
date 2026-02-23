package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {
    List<JobOffer> findByCreatorId(UUID creatorId);

    List<JobOffer> findByWorkerId(UUID workerId);
}

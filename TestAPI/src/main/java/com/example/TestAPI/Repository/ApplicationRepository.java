package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Application;
import com.example.TestAPI.Model.Enum.ApplicationStatus;
import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByJobOrderByCreatedAtDesc(JobOffer job);

    List<Application> findByJobAndStatus(JobOffer job, ApplicationStatus status);

    Optional<Application> findByJobAndWorker(JobOffer job, User worker);

    List<Application> findByWorkerOrderByCreatedAtDesc(User worker);

    List<Application> findAllByOrderByCreatedAtDesc();

    boolean existsByJobAndWorker(JobOffer job, User worker);

    long countByJobAndStatus(JobOffer job, ApplicationStatus status);

    long countByJob_Creator(User creator);

    long countByWorker(User worker);

    long countByStatus(ApplicationStatus status);
}

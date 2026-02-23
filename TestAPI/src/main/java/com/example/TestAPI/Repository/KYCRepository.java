package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KYCRepository extends JpaRepository<KycDocument, UUID> {
    Optional<KycDocument> findByUserId(UUID userId);
}
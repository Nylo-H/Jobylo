package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    Optional<KycDocument> findByUserId(UUID userId);

    List<KycDocument> findByStatus(KycStatus status);

    List<KycDocument> findByStatusOrderBySubmittedAtDesc(KycStatus status);

    List<KycDocument> findAllByOrderBySubmittedAtDesc();
}

package com.example.TestAPI.Service.Kyc;

import com.example.TestAPI.DTO.Kyc.KycDocumentResponse;
import com.example.TestAPI.DTO.Kyc.KycSubmissionRequest;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.Enum.KycDocumentType;
import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.KycDocument;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.KycDocumentRepository;
import com.example.TestAPI.Repository.UserRepository;
import com.example.TestAPI.Service.Audit.AuditService;
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
public class KYCServiceImpl implements KYCService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public KycDocumentResponse submitKYC(User user, KycSubmissionRequest request) {
        if (user.getKycStatus() == KycStatus.VERIFIED) {
            throw new BusinessException("Votre identité est déjà vérifiée", ErrorCode.BAD_REQUEST);
        }

        KycDocument document = KycDocument.builder()
                .user(user)
                .fileUrl(request.fileUrl())
                .documentType(KycDocumentType.valueOf(request.documentType()))
                .submittedAt(new Date())
                .status(KycStatus.PENDING)
                .build();

        document = kycDocumentRepository.save(document);
        user.setKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        auditService.log(user, ActionType.KYC_SUBMITTED, "Doc: " + document.getId());
        return toResponse(document);
    }

    @Override
    public KycDocumentResponse approveKYC(UUID documentId, User admin) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document KYC introuvable", ErrorCode.NOT_FOUND));

        document.setStatus(KycStatus.VERIFIED);
        document.setVerifiedBy(admin);
        document = kycDocumentRepository.save(document);

        User user = document.getUser();
        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        auditService.log(admin, ActionType.KYC_APPROVED, "User: " + user.getId() + " Doc: " + documentId);
        return toResponse(document);
    }

    @Override
    public KycDocumentResponse rejectKYC(UUID documentId, User admin, String reason) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document KYC introuvable", ErrorCode.NOT_FOUND));

        document.setStatus(KycStatus.REJECTED);
        document.setVerifiedBy(admin);
        document.setRejectionReason(reason);
        document = kycDocumentRepository.save(document);

        User user = document.getUser();
        user.setKycStatus(KycStatus.REJECTED);
        userRepository.save(user);

        auditService.log(admin, ActionType.KYC_REJECTED, "User: " + user.getId() + " Doc: " + documentId + " Motif: " + reason);
        return toResponse(document);
    }

    @Override
    public KycDocumentResponse getMyKyc(User user) {
        return kycDocumentRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public List<KycDocumentResponse> getAllKYCs() {
        return kycDocumentRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<KycDocumentResponse> getAllKYCs(KycStatus status) {
        return kycDocumentRepository.findByStatusOrderBySubmittedAtDesc(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private KycDocumentResponse toResponse(KycDocument doc) {
        return new KycDocumentResponse(
                doc.getId(),
                doc.getUser().getId(),
                doc.getFileUrl(),
                doc.getDocumentType().name(),
                doc.getStatus().name(),
                doc.getVerifiedBy() != null ? doc.getVerifiedBy().getId() : null,
                doc.getSubmittedAt(),
                doc.getRejectionReason()
        );
    }
}

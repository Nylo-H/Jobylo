package com.example.TestAPI.Service.Kyc;

import com.example.TestAPI.DTO.Kyc.KycDocumentResponse;
import com.example.TestAPI.DTO.Kyc.KycSubmissionRequest;
import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface KYCService {
    KycDocumentResponse submitKYC(User user, KycSubmissionRequest request);
    KycDocumentResponse approveKYC(UUID documentId, User admin);
    KycDocumentResponse rejectKYC(UUID documentId, User admin, String reason);
    KycDocumentResponse getMyKyc(User user);
    List<KycDocumentResponse> getAllKYCs();
    List<KycDocumentResponse> getAllKYCs(KycStatus status);
}

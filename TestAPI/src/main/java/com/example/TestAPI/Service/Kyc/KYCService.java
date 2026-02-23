package com.example.TestAPI.Service.Kyc;

import com.example.TestAPI.DTO.Kyc.KycResponse;
import com.example.TestAPI.Model.KYC;

import java.util.List;
import java.util.UUID;

public interface KYCService {
    KycResponse submitKYC(KYC kyc);
    KycResponse getKYC(UUID id);
    List<KycResponse> getAllKYCs();
    KycResponse approveKYC(UUID id);
    KycResponse rejectKYC(UUID id);
}

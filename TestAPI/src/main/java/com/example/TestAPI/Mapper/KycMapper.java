package com.example.TestAPI.Mapper;

import com.example.TestAPI.DTO.Kyc.KycResponse;
import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.KYC;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KycMapper {

    KycResponse toDTO(KYC kyc);

    @Mapping(target = "user", ignore = true)
    KYC toEntity(KycResponse dto);

    default KycStatus mapStatus(String status) {
        return KycStatus.valueOf(status);
    }

    default String mapStatus(KycStatus status) {
        return status.name();
    }
}
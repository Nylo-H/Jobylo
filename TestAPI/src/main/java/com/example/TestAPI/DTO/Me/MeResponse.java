package com.example.TestAPI.DTO.Me;

import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.Enum.Role;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String username,
        String email,
        Role role,
        boolean verified,
        KycStatus kycStatus,
        String photoProfil,
        Double averageRating,
        Integer totalRatings
) {}

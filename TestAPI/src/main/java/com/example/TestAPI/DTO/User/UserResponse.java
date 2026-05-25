package com.example.TestAPI.DTO.User;

import com.example.TestAPI.Model.Enum.KycStatus;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String photoProfile,
        String role,
        boolean verified,
        KycStatus kycStatus
) {}
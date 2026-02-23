package com.example.TestAPI.DTO.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String photoProfile,
        String role
) {}
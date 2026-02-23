package com.example.TestAPI.DTO.User;

public record UserUpdateRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String photoProfile
) {}
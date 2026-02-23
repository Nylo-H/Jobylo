package com.example.TestAPI.DTO.User;

public record UserCreateRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String photoProfile
) {}
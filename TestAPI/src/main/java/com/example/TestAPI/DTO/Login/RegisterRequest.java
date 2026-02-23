package com.example.TestAPI.DTO.Login;

public record RegisterRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String password
) {}
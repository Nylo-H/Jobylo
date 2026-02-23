package com.example.TestAPI.DTO.Login;

import java.time.LocalDateTime;

public record LoginResponse(String accesstoken, String refreshtoken) {}
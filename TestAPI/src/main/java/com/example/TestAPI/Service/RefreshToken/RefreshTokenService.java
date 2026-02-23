package com.example.TestAPI.Service.RefreshToken;

import com.example.TestAPI.Model.RefreshToken;
import com.example.TestAPI.Model.User;

public interface RefreshTokenService {
    public RefreshToken createRefreshToken(User user);
    public RefreshToken validateRefreshToken(String token);
}

package com.example.TestAPI.Service.Auth;

import com.example.TestAPI.DTO.Login.LoginRequest;
import com.example.TestAPI.DTO.Login.LoginResponse;
import com.example.TestAPI.DTO.Login.RegisterRequest;
import com.example.TestAPI.DTO.Me.MeResponse;
import com.example.TestAPI.DTO.Token.RefreshRequest;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.DTO.User.UserStatsResponse;
import com.example.TestAPI.Model.User;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse verifyOtp(String email, String code);
    void resendOtp(String email);
    LoginResponse refreshToken(@RequestBody  RefreshRequest request);
    MeResponse getCurrentUser(User user);
    MeResponse updateProfilePhoto(User user, MultipartFile file);
    UserStatsResponse getUserStats(User user);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);
}
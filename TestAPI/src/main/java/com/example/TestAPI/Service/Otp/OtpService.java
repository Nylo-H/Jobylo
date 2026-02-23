package com.example.TestAPI.Service.Otp;

import com.example.TestAPI.Model.User;

public interface OtpService {

    void generateAndSendOtp(User user); // uniformisé avec le Impl

    boolean verifyOtp(String email, String code); // on vérifie par email
}

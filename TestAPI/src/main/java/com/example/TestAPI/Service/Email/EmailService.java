package com.example.TestAPI.Service.Email;

import com.example.TestAPI.Model.User;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}

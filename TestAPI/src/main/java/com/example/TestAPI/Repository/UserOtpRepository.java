package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.User;
import com.example.TestAPI.Model.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserOtpRepository extends JpaRepository<UserOtp, UUID> {
    Optional<UserOtp> findByUser(User user);
}
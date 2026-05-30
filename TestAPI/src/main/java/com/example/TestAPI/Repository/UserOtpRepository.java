package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.User;
import com.example.TestAPI.Model.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserOtpRepository extends JpaRepository<UserOtp, UUID> {
    Optional<UserOtp> findByUser(User user);

    @Modifying
    @Query("DELETE FROM UserOtp o WHERE o.user = ?1")
    void deleteByUser(User user);
}
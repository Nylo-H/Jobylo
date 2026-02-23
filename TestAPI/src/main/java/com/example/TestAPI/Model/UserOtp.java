package com.example.TestAPI.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOtp {
    @Id
    @GeneratedValue
    private UUID id;

    private String code; // OTP 6 chiffres
    private LocalDateTime expiry;

    @OneToOne
    private User user;
}
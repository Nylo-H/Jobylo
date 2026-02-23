package com.example.TestAPI.Service.Otp;

import com.example.TestAPI.Model.User;
import com.example.TestAPI.Model.UserOtp;
import com.example.TestAPI.Repository.UserOtpRepository;
import com.example.TestAPI.Repository.UserRepository;
import com.example.TestAPI.Service.Email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserOtpRepository otpRepo;
    private final EmailService emailService;
    private final UserRepository userRepo;

    @Override
    public void generateAndSendOtp(User user) {
        String otp = String.valueOf(new Random().nextInt(900_000) + 100_000); // 6 chiffres

        UserOtp userOtp = UserOtp.builder()
                .user(user)
                .code(otp)
                .expiry(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepo.save(userOtp);

        emailService.sendEmail(user.getEmail(), "Votre code OTP", "Votre code OTP est : " + otp);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserOtp otp = otpRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("OTP non trouvé"));

        if (otp.getExpiry().isBefore(LocalDateTime.now())) return false;
        if (!otp.getCode().equals(code)) return false;

        user.setVerified(true);
        userRepo.save(user);
        otpRepo.delete(otp);

        return true;
    }
}

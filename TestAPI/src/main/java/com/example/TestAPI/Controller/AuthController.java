package com.example.TestAPI.Controller;


import com.example.TestAPI.DTO.Email.OtpRequest;
import com.example.TestAPI.DTO.Login.LoginRequest;
import com.example.TestAPI.DTO.Login.LoginResponse;
import com.example.TestAPI.DTO.Login.RegisterRequest;
import com.example.TestAPI.DTO.Me.MeResponse;
import com.example.TestAPI.DTO.Success.SuccessResponse;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.Model.RefreshToken;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Security.JwtService;
import com.example.TestAPI.Service.Auth.AuthService;
import com.example.TestAPI.Service.RefreshToken.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest request) {
        // L’OTP est généré et envoyé automatiquement par le service
        return  authService.register(request);
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpRequest request) {
        boolean success = authService.verifyOtp(request.email(), request.otp());
        return success ? "Compte vérifié avec succès" : "OTP invalide ou expiré";
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<SuccessResponse> resendOtp(@RequestParam String username) {
        authService.resendOtp(username);
        return ResponseEntity.ok(new SuccessResponse("Nouveau code OTP envoyé"));
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse tokens = authService.login(request);

        ResponseCookie refresCookie = ResponseCookie.from("refreshToken", tokens.refreshtoken())
                .httpOnly(true)
                .secure(false) //Ici je dois mettre true en production cause https
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", refresCookie.toString());
        return ResponseEntity.ok(new LoginResponse(tokens.accesstoken(), null));
    }

    @PostMapping("/refresh")
    public LoginResponse refreshToken(@CookieValue("refreshToken") String refreshToken)  {
//        return authService.refreshToken(request);
        RefreshToken rt = refreshTokenService.validateRefreshToken(refreshToken);
        String newAccessToken = jwtService.generateToken(rt.getUser().getUsername());
        return new LoginResponse(newAccessToken, null);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MeResponse me(@AuthenticationPrincipal User user) {
        return authService.getCurrentUser(user);
    }

}
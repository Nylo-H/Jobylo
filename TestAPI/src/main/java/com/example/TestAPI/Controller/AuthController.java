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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.TestAPI.Service.RefreshToken.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

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
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody OtpRequest request) {
        LoginResponse response = authService.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<SuccessResponse> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
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
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public LoginResponse refreshToken(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {

        RefreshToken rt = refreshTokenService.validateRefreshToken(refreshToken);
        String newAccessToken = jwtService.generateToken(rt.getUser().getUsername());
        RefreshToken newRt = refreshTokenService.createRefreshToken(rt.getUser());

        ResponseCookie refresCookie = ResponseCookie.from("refreshToken", newRt.getToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", refresCookie.toString());
        return new LoginResponse(newAccessToken, newRt.getToken(), rt.getUser().isVerified());
    }

    @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public MeResponse updateProfilePhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        return authService.updateProfilePhoto(user, file);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MeResponse me(@AuthenticationPrincipal User user) {
        return authService.getCurrentUser(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "Si cet email existe, un code de réinitialisation a été envoyé"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

}

record ForgotPasswordRequest(@Email @NotBlank String email) {}

record ResetPasswordRequest(@Email @NotBlank String email, @NotBlank String otp, @NotBlank @Size(min = 6) String newPassword) {}
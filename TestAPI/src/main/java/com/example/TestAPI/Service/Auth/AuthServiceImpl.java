package com.example.TestAPI.Service.Auth;

import com.example.TestAPI.DTO.Login.LoginRequest;
import com.example.TestAPI.DTO.Login.LoginResponse;
import com.example.TestAPI.DTO.Login.RegisterRequest;
import com.example.TestAPI.DTO.Me.MeResponse;
import com.example.TestAPI.DTO.Token.RefreshRequest;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.Mapper.UserMapper;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.Enum.Role;
import com.example.TestAPI.Model.RefreshToken;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.UserRepository;
import com.example.TestAPI.Security.JwtService;
import com.example.TestAPI.Service.Audit.AuditService;
import com.example.TestAPI.Service.Otp.OtpService;
import com.example.TestAPI.Service.RefreshToken.RefreshTokenService;
import com.example.TestAPI.exception.InvalidPasswordException;
import com.example.TestAPI.exception.UserAlreadyVerifiedException;
import com.example.TestAPI.exception.UserNotFoundException;
import com.example.TestAPI.exception.UserNotVerifiedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public AuthServiceImpl(UserRepository userRepo, JwtService jwtService, UserMapper userMapper, OtpService otpService, RefreshTokenService refreshTokenService, AuditService auditService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {

        // Vérification des champs obligatoires
        if (request.firstName() == null || request.firstName().isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire");
        }
        if (request.lastName() == null || request.lastName().isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }

        // Vérification des doublons
        if (userRepo.existsByUsername(request.username())) {
            throw new RuntimeException("Nom d'utilisateur déjà utilisé");
        }
        if (userRepo.existsByEmail(request.email())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Création de l'utilisateur
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .verified(false)
                .kycStatus(KycStatus.PENDING)
                .build();

        User saved = userRepo.save(user);

        auditService.log(saved, ActionType.REGISTER, "User: " + saved.getId());

        // Génération OTP après création
        otpService.generateAndSendOtp(saved);

        return userMapper.toDTO(saved);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        return otpService.verifyOtp(email, code);
    }

    @Transactional
    @Override
    public LoginResponse login(LoginRequest request)  {
        User user = userRepo.findByUsername(request.username())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }

        auditService.log(user, ActionType.LOGIN, "User: " + user.getId());
        String acesstoken = jwtService.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new LoginResponse(acesstoken, refreshToken.getToken());
    }

    public void resendOtp(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (user.isVerified()) {
            throw new UserAlreadyVerifiedException();
        }

        otpService.generateAndSendOtp(user);
    }

    @Override
    public LoginResponse refreshToken(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        String newAccessToken = jwtService.generateToken(refreshToken.getUser().getUsername());
        return new LoginResponse(newAccessToken, refreshToken.getToken());
    }

    @Override
    public MeResponse getCurrentUser(User user) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isVerified(),
                user.getKycStatus(),
                user.getPhotoProfil()
        );
    }


}

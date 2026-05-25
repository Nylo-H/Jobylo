package com.example.TestAPI.Service.RefreshToken;


import com.example.TestAPI.Model.RefreshToken;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.RefreshTokenRepository;
import com.example.TestAPI.exception.BusinessException;
import com.example.TestAPI.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository repo;

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = repo.findByUser(user)
                .orElseGet(() -> {
                    RefreshToken rt = new RefreshToken();
                    rt.setUser(user);
                    return rt;
                });

        token.setToken(UUID.randomUUID().toString());
        token.setExpiration(LocalDateTime.now().plusDays(7));

        return repo.save(token);
    }

    @Override
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = repo.findByToken(token).orElseThrow(()-> new BusinessException("Refresh Token Invalide", ErrorCode.UNAUTHORIZED));
        if(refreshToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh Token Expiré", ErrorCode.UNAUTHORIZED);
        }
        return refreshToken;
    }
}

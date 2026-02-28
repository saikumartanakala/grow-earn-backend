package com.growearn.service;

import com.growearn.entity.RefreshToken;
import com.growearn.entity.RevokedToken;
import com.growearn.entity.User;
import com.growearn.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration-days:7}")
    private long refreshDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(User user) {
        String token = generateOpaqueToken();
        String tokenHash = RevokedToken.hashToken(token);
        LocalDateTime expiry = LocalDateTime.now().plusDays(refreshDays);
        RefreshToken refreshToken = new RefreshToken(user.getId(), tokenHash, expiry);
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public Optional<RefreshToken> rotate(String rawToken) {
        String hash = RevokedToken.hashToken(rawToken);
        Optional<RefreshToken> existing = refreshTokenRepository.findByTokenHash(hash);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken token = existing.get();
        if (token.getRevoked() || token.isExpired()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            return Optional.empty();
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return Optional.of(token);
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = RevokedToken.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

package com.growearn.controller;

import com.growearn.dto.AuthResponseDTO;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.security.JwtUtil;
import com.growearn.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthRefreshController {

    private static final Logger logger = LoggerFactory.getLogger(AuthRefreshController.class);

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${security.refresh-cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${security.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${security.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${security.refresh-cookie.path:/}")
    private String refreshCookiePath;

    @Value("${jwt.refresh.expiration-days:7}")
    private long refreshCookieDays;

    public AuthRefreshController(RefreshTokenService refreshTokenService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String raw = readCookie(request, refreshCookieName);
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("message", "Missing refresh token"));
        }
        var rotated = refreshTokenService.rotate(raw);
        if (rotated.isEmpty()) {
            logger.warn("refresh_reuse_attempt token={}", raw.substring(0, Math.min(10, raw.length())));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("message", "Invalid refresh token"));
        }
        Long userId = rotated.get().getUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("message", "User not found"));
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String newRefresh = refreshTokenService.createRefreshToken(user);
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, newRefresh)
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path(refreshCookiePath)
            .sameSite(refreshCookieSameSite)
            .maxAge(Duration.ofDays(refreshCookieDays))
            .build();
        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(new AuthResponseDTO(accessToken, null, user.getRole().name()));
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}

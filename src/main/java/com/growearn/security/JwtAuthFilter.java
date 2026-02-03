package com.growearn.security;

import com.growearn.entity.AccountStatus;
import com.growearn.entity.User;
import com.growearn.entity.RevokedToken;
import com.growearn.repository.UserRepository;
import com.growearn.repository.RevokedTokenRepository;
import com.growearn.repository.DeviceRegistryRepository;
import com.growearn.entity.DeviceRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;
    private final DeviceRegistryRepository deviceRegistryRepository;

    /**
     * Extract JWT token from Authorization header
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public JwtAuthFilter(UserRepository userRepository, JwtUtil jwtUtil, RevokedTokenRepository revokedTokenRepository, DeviceRegistryRepository deviceRegistryRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.revokedTokenRepository = revokedTokenRepository;
        this.deviceRegistryRepository = deviceRegistryRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }
            if (isTokenRevoked(token)) {
                logSecurity("REJECT", "Token revoked", null, request);
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logSecurity("REJECT", "User not found", userId, request);
                sendUnauthorizedResponse(response, "User not found");
                return;
            }
            User user = userOpt.get();
            // Device fingerprint enforcement
            String deviceFingerprint = request.getHeader("X-Device-Fingerprint");
            if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
                logSecurity("REJECT", "Missing device fingerprint", userId, request);
                sendUnauthorizedResponse(response, "Device fingerprint required");
                return;
            }
            if (user.getDeviceFingerprint() == null || !user.getDeviceFingerprint().equals(deviceFingerprint)) {
                // Revoke token, log, and block
                revokeToken(token, user, "Device fingerprint mismatch");
                logSecurity("REVOKE", "Device fingerprint mismatch", userId, request);
                sendUnauthorizedResponse(response, "Device mismatch");
                return;
            }
            // Account status enforcement
            AccountStatus status = user.getStatus() != null ? user.getStatus() : AccountStatus.ACTIVE;
            if (status == AccountStatus.BANNED) {
                logSecurity("REJECT", "User banned", userId, request);
                sendForbiddenResponse(response, "Your account has been permanently banned.");
                return;
            }
            if (status == AccountStatus.SUSPENDED) {
                if (user.getSuspensionUntil() == null || LocalDateTime.now().isBefore(user.getSuspensionUntil())) {
                    String message = user.getSuspensionUntil() != null ? "Your account is suspended until " + user.getSuspensionUntil() : "Your account is suspended. Please contact support.";
                    logSecurity("REJECT", "User suspended", userId, request);
                    sendForbiddenResponse(response, message);
                    return;
                } else {
                    // Suspension expired, auto-reactivate
                    user.setStatus(AccountStatus.ACTIVE);
                    user.setSuspensionUntil(null);
                    userRepository.save(user);
                    logSecurity("INFO", "User suspension expired, reactivated", userId, request);
                }
            }
            // Normal role/authority setup
            String raw = role != null ? role.toUpperCase().trim() : "<none>";
            String mapped;
            switch (raw) {
                case "USER", "VIEWER", "VIEWER_ROLE" -> mapped = "VIEWER";
                case "CREATOR", "CONTENT_CREATOR" -> mapped = "CREATOR";
                case "ADMIN", "ADMINISTRATOR" -> mapped = "ADMIN";
                default -> mapped = raw.replaceFirst("^ROLE_", "");
            }
            String normalized = mapped.startsWith("ROLE_") ? mapped : "ROLE_" + mapped;
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(normalized));
            String principal = String.valueOf(userId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            try {
                WebAuthenticationDetailsSource src = new WebAuthenticationDetailsSource();
                authentication.setDetails(src.buildDetails(request));
            } catch (Exception ignored) {}
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logSecurity("ACCEPT", "Token accepted", userId, request);
        } catch (Exception ex) {
            logSecurity("ERROR", "Exception processing token: " + ex.getMessage(), null, request);
        }
        filterChain.doFilter(request, response);
    }
    private void revokeToken(String jwt, User user, String reason) {
        RevokedToken revoked = new RevokedToken();
        revoked.setTokenHash(RevokedToken.hashToken(jwt));
        revoked.setUserId(user.getId());
        revoked.setToken(jwt);
        revoked.setReason(reason);
        revoked.setRevokedAt(LocalDateTime.now());
        revokedTokenRepository.save(revoked);
    }

    private void logSecurity(String action, String message, Long userId, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();
        String device = request.getHeader("X-Device-Fingerprint");
        String userAgent = request.getHeader("User-Agent");
        System.out.println(String.format("{\"event\":\"jwt_security\",\"action\":\"%s\",\"userId\":%s,\"ip\":\"%s\",\"device\":\"%s\",\"userAgent\":\"%s\",\"message\":\"%s\"}", action, userId, ip, device, userAgent, message));
    }

    // ...existing code for isTokenRevoked, sendUnauthorizedResponse, sendForbiddenResponse...

    /**
     * Check if a token is revoked
     */
    private boolean isTokenRevoked(String token) {
        try {
            String tokenHash = RevokedToken.hashToken(token);
            return revokedTokenRepository.isTokenRevoked(tokenHash);
        } catch (Exception e) {
            // If table doesn't exist yet, token is not revoked
            return false;
        }
    }

    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }

    /**
     * Send 403 Forbidden response
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"forbidden\",\"message\":\"" + message + "\"}");
    }
}

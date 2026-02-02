package com.growearn.security;

import com.growearn.entity.AccountStatus;
import com.growearn.entity.RevokedToken;
import com.growearn.entity.User;
import com.growearn.repository.RevokedTokenRepository;
import com.growearn.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, 
                        RevokedTokenRepository revokedTokenRepository,
                        UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.revokedTokenRepository = revokedTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow unauthenticated access to auth endpoints
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token present — do not set authentication; let Spring handle access control
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if token is revoked
            if (isTokenRevoked(token)) {
                System.out.println("[JwtAuthFilter] Token is revoked");
                sendUnauthorizedResponse(response, "Token has been revoked. Please login again.");
                return;
            }

            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            if (role == null || role.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check user status (suspended/banned)
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                System.out.println("[JwtAuthFilter] User not found: " + userId);
                sendUnauthorizedResponse(response, "User not found");
                return;
            }

            User user = userOpt.get();
            
            // Handle NULL status as ACTIVE (for existing users before migration)
            AccountStatus status = user.getStatus();
            if (status == null) {
                status = AccountStatus.ACTIVE;
            }
            
            // Check if user is banned
            if (status == AccountStatus.BANNED) {
                System.out.println("[JwtAuthFilter] User is banned: " + userId);
                sendForbiddenResponse(response, "Your account has been permanently banned.");
                return;
            }

            // Check if user is suspended
            if (status == AccountStatus.SUSPENDED) {
                if (user.getSuspensionUntil() == null || LocalDateTime.now().isBefore(user.getSuspensionUntil())) {
                    String message = user.getSuspensionUntil() != null 
                        ? "Your account is suspended until " + user.getSuspensionUntil()
                        : "Your account is suspended. Please contact support.";
                    System.out.println("[JwtAuthFilter] User is suspended: " + userId);
                    sendForbiddenResponse(response, message);
                    return;
                } else {
                    // Suspension expired, auto-reactivate
                    user.setStatus(AccountStatus.ACTIVE);
                    user.setSuspensionUntil(null);
                    userRepository.save(user);
                    System.out.println("[JwtAuthFilter] User suspension expired, reactivated: " + userId);
                }
            }

            // Normalize role values: accept synonyms and ensure Spring Security `ROLE_` prefix
            String raw = role.toUpperCase().trim();
            // Map common synonyms to canonical roles
            String mapped;
            switch (raw) {
                case "USER", "VIEWER", "VIEWER_ROLE" -> mapped = "VIEWER";
                case "CREATOR", "CONTENT_CREATOR" -> mapped = "CREATOR";
                case "ADMIN", "ADMINISTRATOR" -> mapped = "ADMIN";
                default -> mapped = raw.replaceFirst("^ROLE_", "");
            }
            String normalized = mapped.startsWith("ROLE_") ? mapped : "ROLE_" + mapped;
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(normalized));

            // Use userId string as principal and attach request details
            String principal = String.valueOf(userId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            try {
                // set details so downstream code can inspect remote IP / session if needed
                org.springframework.security.web.authentication.WebAuthenticationDetailsSource src = new org.springframework.security.web.authentication.WebAuthenticationDetailsSource();
                authentication.setDetails(src.buildDetails(request));
            } catch (Exception ignored) {}
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Log accepted token principal & role for debugging (no token value)
            System.out.println("[JwtAuthFilter] Accepted token for userId=" + principal + " role=" + mapped);
        } catch (Exception ex) {
            // On any exception, do not set authentication and continue — Spring Security will deny access if required
            System.out.println("[JwtAuthFilter] Exception processing token: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

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

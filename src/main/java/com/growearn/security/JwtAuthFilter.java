package com.growearn.security;

import com.growearn.entity.AccountStatus;
import com.growearn.entity.User;
import com.growearn.entity.RevokedToken;
import com.growearn.repository.UserRepository;
import com.growearn.repository.RevokedTokenRepository;
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

    public JwtAuthFilter(UserRepository userRepository, JwtUtil jwtUtil, RevokedTokenRepository revokedTokenRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);
            System.out.println("[JwtAuthFilter] Incoming request URI: " + request.getRequestURI());
            System.out.println("[JwtAuthFilter] Raw token: " + (token != null ? token : "<none>"));
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (isTokenRevoked(token)) {
                System.out.println("[JwtAuthFilter] Token revoked");
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }

            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            System.out.println("[JwtAuthFilter] Extracted userId: " + userId);
            System.out.println("[JwtAuthFilter] Extracted role: " + role);

            // Check user status (suspended/banned)
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                System.out.println("[JwtAuthFilter] User not found: " + userId);
                sendUnauthorizedResponse(response, "User not found");
                return;
            }

            User user = userOpt.get();
            System.out.println("[JwtAuthFilter] DB user role: " + (user.getRole() != null ? user.getRole().name() : "<none>"));
            System.out.println("[JwtAuthFilter] DB user status: " + (user.getStatus() != null ? user.getStatus().name() : "<none>"));

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
            String raw = role != null ? role.toUpperCase().trim() : "<none>";
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
                WebAuthenticationDetailsSource src = new WebAuthenticationDetailsSource();
                authentication.setDetails(src.buildDetails(request));
            } catch (Exception ignored) {}
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Log accepted token principal & role for debugging (no token value)
            System.out.println("[JwtAuthFilter] Accepted token for userId=" + principal + " role=" + mapped);
        } catch (Exception ex) {
            System.out.println("[JwtAuthFilter] Exception processing token: " + ex.getMessage());
        }
        filterChain.doFilter(request, response);
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

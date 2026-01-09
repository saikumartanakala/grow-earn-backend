package com.growearn.security;

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
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
            System.out.println("[JwtFilter] Request URI: " + path);

        // skip auth
        if (path.startsWith("/api/auth/")) {
                System.out.println("[JwtFilter] Skipping auth for: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
            System.out.println("[JwtFilter] Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtFilter] Missing or invalid Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);
            System.out.println("[JwtFilter] JWT token: " + token);

        if (!jwtUtil.validateToken(token)) {
            System.out.println("[JwtFilter] Invalid JWT token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid JWT token\"}");
            return;
        }

        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
            System.out.println("[JwtFilter] Extracted userId: " + userId + ", role: " + role);
        if (role == null) {
            System.out.println("[JwtFilter] Role is null in JWT");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Role is missing in JWT\"}");
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("[JwtFilter] Authentication set for userId: " + userId + ", authorities: ROLE_" + role.toUpperCase());

        filterChain.doFilter(request, response);
    }
}

package com.growearn.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtFilter, RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                // Allow preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ✅ ALLOW AUTH APIs
                .requestMatchers("/api/auth/**").permitAll()

                // ✅ ALLOW TEST APIs (for testing)
                .requestMatchers("/api/test/**").permitAll()

                // ✅ ALLOW WEBHOOKS
                .requestMatchers("/webhooks/**").permitAll()

                // ✅ PROFILE ENDPOINTS: Any authenticated user
                .requestMatchers("/api/profile/**").hasAnyRole("USER", "VIEWER", "CREATOR", "ADMIN")

                // ✅ VIEWER ENDPOINTS: USER or VIEWER role
                .requestMatchers("/api/viewer/**").hasAnyRole("USER", "VIEWER")
                
                // ✅ TASKS ENDPOINTS: USER or VIEWER role (for active tasks listing)
                .requestMatchers("/api/tasks/**").hasAnyRole("USER", "VIEWER")

                // ✅ Explicitly allow validate-details for CREATOR
                .requestMatchers("/api/creator/campaign/validate-details").hasRole("CREATOR")

                // ✅ CREATOR ENDPOINTS: Only CREATOR role
                .requestMatchers("/api/creator/**").hasRole("CREATOR")

                // ✅ ADMIN ENDPOINTS: Only ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 🔐 ALL OTHER APIS NEED TOKEN
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> originPatterns = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(originPatterns);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

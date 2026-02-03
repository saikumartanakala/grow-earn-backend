package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.RevokedTokenRepository;
import com.growearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    public User save(User user) {
        return userRepository.save(user);
    }

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, 
                       RevokedTokenRepository revokedTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Activate a user account
     */
    @Transactional
    public User activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setStatus(AccountStatus.ACTIVE);
        user.setSuspensionUntil(null);
        logger.info("User {} activated", userId);
        return userRepository.save(user);
    }

    /**
     * Deactivate a user account (soft disable)
     */
    @Transactional
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setStatus(AccountStatus.SUSPENDED);
        user.setSuspensionUntil(null); // Indefinite until reactivated
        logger.info("User {} deactivated", userId);
        return userRepository.save(user);
    }

    /**
     * Suspend a user for a specific duration
     */
    @Transactional
    public User suspendUser(Long userId, int days, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setStatus(AccountStatus.SUSPENDED);
        user.setSuspensionUntil(LocalDateTime.now().plusDays(days));
        logger.info("User {} suspended for {} days. Reason: {}", userId, days, reason);
        return userRepository.save(user);
    }

    /**
     * Permanently ban a user
     */
    @Transactional
    public User banUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setStatus(AccountStatus.BANNED);
        user.setSuspensionUntil(null);
        logger.info("User {} permanently banned. Reason: {}", userId, reason);
        return userRepository.save(user);
    }

    /**
     * Reset user password
     */
    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        logger.info("Password reset for user {}", userId);
        return userRepository.save(user);
    }

    /**
     * Verify a creator account
     */
    @Transactional
    public User verifyCreator(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        if (user.getRole() != Role.CREATOR) {
            throw new RuntimeException("User is not a creator");
        }
        
        user.setIsVerified(true);
        logger.info("Creator {} verified", userId);
        return userRepository.save(user);
    }

    /**
     * Unverify a creator account
     */
    @Transactional
    public User unverifyCreator(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setIsVerified(false);
        logger.info("Creator {} unverified", userId);
        return userRepository.save(user);
    }

    /**
     * Force logout by revoking all user's tokens
     */
    @Transactional
    public void forceLogout(Long userId, Long adminId, String reason) {
        // We'll use a marker token to indicate all tokens for this user are revoked
        RevokedToken revokedToken = new RevokedToken(
            userId, 
            "ALL_TOKENS_REVOKED_" + System.currentTimeMillis(), 
            adminId, 
            reason != null ? reason : "Force logout by admin"
        );
        revokedTokenRepository.save(revokedToken);
        logger.info("All tokens revoked for user {} by admin {}", userId, adminId);
    }

    /**
     * Revoke a specific token
     */
    @Transactional
    public void revokeToken(Long userId, String token, Long adminId, String reason) {
        String tokenHash = RevokedToken.hashToken(token);
        if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
            RevokedToken revokedToken = new RevokedToken(userId, token, adminId, reason);
            revokedTokenRepository.save(revokedToken);
            logger.info("Token revoked for user {}", userId);
        }
    }

    /**
     * Check if a token is revoked
     */
    public boolean isTokenRevoked(String token) {
        String tokenHash = RevokedToken.hashToken(token);
        return revokedTokenRepository.isTokenRevoked(tokenHash);
    }

    /**
     * Check if user can login (not suspended or banned)
     */
    public boolean canUserLogin(User user) {
        if (user == null) return false;
        
        if (user.getStatus() == AccountStatus.BANNED) {
            return false;
        }
        
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            if (user.getSuspensionUntil() == null) {
                return false; // Indefinitely suspended
            }
            // Check if suspension has expired
            if (LocalDateTime.now().isAfter(user.getSuspensionUntil())) {
                // Auto-activate expired suspension
                user.setStatus(AccountStatus.ACTIVE);
                user.setSuspensionUntil(null);
                userRepository.save(user);
                return true;
            }
            return false;
        }
        
        return user.getStatus() == AccountStatus.ACTIVE;
    }

    /**
     * Get user status details
     */
    public String getUserStatusMessage(User user) {
        if (user == null) return "User not found";
        
        switch (user.getStatus()) {
            case BANNED:
                return "Your account has been permanently banned.";
            case SUSPENDED:
                if (user.getSuspensionUntil() != null) {
                    return "Your account is suspended until " + user.getSuspensionUntil();
                }
                return "Your account is suspended. Please contact support.";
            case ACTIVE:
                return "Account is active";
            default:
                return "Unknown status";
        }
    }
}

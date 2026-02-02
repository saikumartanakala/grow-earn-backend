package com.growearn.repository;

import com.growearn.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    
    boolean existsByTokenHash(String tokenHash);
    
    List<RevokedToken> findByUserId(Long userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken r WHERE r.revokedAt < :before")
    int deleteExpiredTokens(@Param("before") LocalDateTime before);
    
    @Query("SELECT COUNT(r) > 0 FROM RevokedToken r WHERE r.tokenHash = :tokenHash")
    boolean isTokenRevoked(@Param("tokenHash") String tokenHash);
}

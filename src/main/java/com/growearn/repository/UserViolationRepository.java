package com.growearn.repository;

import com.growearn.entity.UserViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserViolationRepository extends JpaRepository<UserViolation, Long> {
    
    List<UserViolation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT COUNT(v) FROM UserViolation v WHERE v.userId = :userId AND v.violationType = 'STRIKE'")
    int countStrikesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(MAX(v.strikeCount), 0) FROM UserViolation v WHERE v.userId = :userId")
    int getMaxStrikeCount(@Param("userId") Long userId);
    
    List<UserViolation> findByViolationType(String violationType);
}

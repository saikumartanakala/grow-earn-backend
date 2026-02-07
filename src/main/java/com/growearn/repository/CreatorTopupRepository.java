package com.growearn.repository;

import com.growearn.entity.CreatorTopup;
import com.growearn.entity.TopupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreatorTopupRepository extends JpaRepository<CreatorTopup, Long> {
    
    Page<CreatorTopup> findByStatus(TopupStatus status, Pageable pageable);
    
    Page<CreatorTopup> findByCreatorId(Long creatorId, Pageable pageable);
    
    List<CreatorTopup> findByCreatorIdAndStatus(Long creatorId, TopupStatus status);
    
    @Query("SELECT ct FROM CreatorTopup ct WHERE ct.status = :status ORDER BY ct.createdAt ASC")
    List<CreatorTopup> findPendingTopupsOrderByDate(@Param("status") TopupStatus status);
    
    @Query("SELECT COUNT(ct) FROM CreatorTopup ct WHERE ct.creatorId = :creatorId AND ct.status = 'PENDING'")
    long countPendingByCreatorId(@Param("creatorId") Long creatorId);
}

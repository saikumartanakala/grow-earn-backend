package com.growearn.repository;

import com.growearn.entity.WithdrawalRequest;
import com.growearn.entity.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    
    Page<WithdrawalRequest> findByStatus(WithdrawalStatus status, Pageable pageable);
    
    Page<WithdrawalRequest> findByUserId(Long userId, Pageable pageable);
    
    List<WithdrawalRequest> findByUserIdAndStatus(Long userId, WithdrawalStatus status);
    
    @Query("SELECT wr FROM WithdrawalRequest wr WHERE wr.status = :status ORDER BY wr.requestedAt ASC")
    List<WithdrawalRequest> findPendingWithdrawalsOrderByDate(@Param("status") WithdrawalStatus status);
    
    @Query("SELECT COUNT(wr) FROM WithdrawalRequest wr WHERE wr.userId = :userId AND wr.status = 'PENDING'")
    long countPendingByUserId(@Param("userId") Long userId);
}

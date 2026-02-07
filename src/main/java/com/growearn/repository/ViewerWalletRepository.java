package com.growearn.repository;

import com.growearn.entity.ViewerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ViewerWalletRepository extends JpaRepository<ViewerWallet, Long> {
    
    Optional<ViewerWallet> findByUserId(Long userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vw FROM ViewerWallet vw WHERE vw.userId = :userId")
    Optional<ViewerWallet> findByUserIdWithLock(@Param("userId") Long userId);
}

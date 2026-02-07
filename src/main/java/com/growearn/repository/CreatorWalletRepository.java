package com.growearn.repository;

import com.growearn.entity.CreatorWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CreatorWalletRepository extends JpaRepository<CreatorWallet, Long> {
    
    Optional<CreatorWallet> findByCreatorId(Long creatorId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cw FROM CreatorWallet cw WHERE cw.creatorId = :creatorId")
    Optional<CreatorWallet> findByCreatorIdWithLock(@Param("creatorId") Long creatorId);
}

package com.growearn.repository;

import com.growearn.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
    Optional<WalletEntity> findByViewerId(Long viewerId);
}

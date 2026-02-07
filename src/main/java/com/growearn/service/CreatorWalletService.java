package com.growearn.service;

import com.growearn.entity.CreatorWallet;
import com.growearn.repository.CreatorWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CreatorWalletService {

    private static final Logger logger = LoggerFactory.getLogger(CreatorWalletService.class);

    private final CreatorWalletRepository creatorWalletRepository;

    public CreatorWalletService(CreatorWalletRepository creatorWalletRepository) {
        this.creatorWalletRepository = creatorWalletRepository;
    }

    /**
     * Get or create wallet for a creator
     */
    @Transactional
    public CreatorWallet getOrCreateWallet(Long creatorId) {
        Optional<CreatorWallet> existingWallet = creatorWalletRepository.findByCreatorId(creatorId);
        if (existingWallet.isPresent()) {
            return existingWallet.get();
        }

        CreatorWallet newWallet = new CreatorWallet(creatorId);
        CreatorWallet saved = creatorWalletRepository.save(newWallet);
        logger.info("Created new wallet for creator: {}", creatorId);
        return saved;
    }

    /**
     * Get wallet balance
     */
    public CreatorWallet getWallet(Long creatorId) {
        return creatorWalletRepository.findByCreatorId(creatorId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for creator: " + creatorId));
    }

    /**
     * Credit balance after admin approval
     */
    @Transactional
    public void creditBalance(Long creatorId, BigDecimal amount) {
        CreatorWallet wallet = creatorWalletRepository.findByCreatorIdWithLock(creatorId)
            .orElseGet(() -> {
                CreatorWallet newWallet = new CreatorWallet(creatorId);
                return creatorWalletRepository.save(newWallet);
            });

        wallet.addToBalance(amount);
        creatorWalletRepository.save(wallet);
        logger.info("Credited {} to balance for creator {}", amount, creatorId);
    }

    /**
     * Lock balance for campaign usage
     */
    @Transactional
    public void lockBalance(Long creatorId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CreatorWallet wallet = creatorWalletRepository.findByCreatorIdWithLock(creatorId)
            .orElseGet(() -> {
                CreatorWallet newWallet = new CreatorWallet(creatorId);
                return creatorWalletRepository.save(newWallet);
            });

        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for campaign");
        }

        wallet.addToLockedBalance(amount);
        creatorWalletRepository.save(wallet);
        logger.info("Locked {} balance for creator {}", amount, creatorId);
    }

    /**
     * Deduct from locked balance (for task payment)
     */
    @Transactional
    public void deductLockedBalance(Long creatorId, BigDecimal amount) {
        CreatorWallet wallet = creatorWalletRepository.findByCreatorIdWithLock(creatorId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for creator: " + creatorId));

        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient locked balance");
        }

        wallet.deductFromLockedBalance(amount);
        wallet.deductFromBalance(amount);
        creatorWalletRepository.save(wallet);
        logger.info("Deducted {} from locked balance for creator {}", amount, creatorId);
    }

    /**
     * Release locked balance back to available (for unused campaign budget)
     */
    @Transactional
    public void releaseLockedBalance(Long creatorId, BigDecimal amount) {
        CreatorWallet wallet = creatorWalletRepository.findByCreatorIdWithLock(creatorId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for creator: " + creatorId));

        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient locked balance to release");
        }

        wallet.deductFromLockedBalance(amount);
        creatorWalletRepository.save(wallet);
        logger.info("Released {} from locked to available balance for creator {}", amount, creatorId);
    }

    /**
     * Check if creator has sufficient balance
     */
    public boolean hasSufficientBalance(Long creatorId, BigDecimal amount) {
        CreatorWallet wallet = creatorWalletRepository.findByCreatorId(creatorId)
            .orElse(new CreatorWallet(creatorId));
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        return available.compareTo(amount) >= 0;
    }

    /**
     * Get available balance
     */
    public BigDecimal getAvailableBalance(Long creatorId) {
        CreatorWallet wallet = creatorWalletRepository.findByCreatorId(creatorId)
            .orElse(new CreatorWallet(creatorId));
        return wallet.getBalance().subtract(wallet.getLockedBalance());
    }
}

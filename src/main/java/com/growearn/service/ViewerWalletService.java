package com.growearn.service;

import com.growearn.entity.ViewerWallet;
import com.growearn.repository.ViewerWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ViewerWalletService {

    private static final Logger logger = LoggerFactory.getLogger(ViewerWalletService.class);

    private final ViewerWalletRepository viewerWalletRepository;

    public ViewerWalletService(ViewerWalletRepository viewerWalletRepository) {
        this.viewerWalletRepository = viewerWalletRepository;
    }

    /**
     * Get or create wallet for a viewer
     */
    @Transactional
    public ViewerWallet getOrCreateWallet(Long userId) {
        Optional<ViewerWallet> existingWallet = viewerWalletRepository.findByUserId(userId);
        if (existingWallet.isPresent()) {
            return existingWallet.get();
        }

        ViewerWallet newWallet = new ViewerWallet(userId);
        ViewerWallet saved = viewerWalletRepository.save(newWallet);
        logger.info("Created new wallet for viewer: {}", userId);
        return saved;
    }

    /**
     * Get wallet balance
     */
    public ViewerWallet getWallet(Long userId) {
        return viewerWalletRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }

    /**
     * Add earnings to locked balance (on task submission)
     */
    @Transactional
    public void addEarnings(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ViewerWallet wallet = viewerWalletRepository.findByUserIdWithLock(userId)
            .orElseGet(() -> {
                ViewerWallet newWallet = new ViewerWallet(userId);
                return viewerWalletRepository.save(newWallet);
            });

        wallet.addToLockedBalance(amount);
        viewerWalletRepository.save(wallet);
        logger.info("Added {} to locked balance for viewer {}", amount, userId);
    }

    /**
     * Release locked balance to available balance
     */
    @Transactional
    public void releaseToBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ViewerWallet wallet = viewerWalletRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        if (wallet.getLockedBalance().compareTo(amount) >= 0) {
            wallet.deductFromLockedBalance(amount);
        } else {
            // Legacy fallback: credit available even if locked balance was not tracked
            logger.warn("Locked balance insufficient for viewer {}. Locked={}, Requested={}. Crediting available only.",
                userId, wallet.getLockedBalance(), amount);
        }
        wallet.addToBalance(amount);
        viewerWalletRepository.save(wallet);
        logger.info("Released {} from locked to available balance for viewer {}", amount, userId);
    }

    /**
     * Remove earnings from locked balance (on rejection)
     */
    @Transactional
    public void removeLockedEarnings(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ViewerWallet wallet = viewerWalletRepository.findByUserIdWithLock(userId)
            .orElseGet(() -> new ViewerWallet(userId));

        BigDecimal locked = wallet.getLockedBalance();
        if (locked.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal deduction = locked.compareTo(amount) >= 0 ? amount : locked;
        wallet.deductFromLockedBalance(deduction);
        viewerWalletRepository.save(wallet);
        logger.info("Removed {} from locked balance for viewer {}", deduction, userId);
    }

    /**
     * Credit available balance directly
     */
    @Transactional
    public void creditBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ViewerWallet wallet = viewerWalletRepository.findByUserIdWithLock(userId)
            .orElseGet(() -> {
                ViewerWallet newWallet = new ViewerWallet(userId);
                return viewerWalletRepository.save(newWallet);
            });

        wallet.addToBalance(amount);
        viewerWalletRepository.save(wallet);
        logger.info("Credited {} to available balance for viewer {}", amount, userId);
    }

    /**
     * Deduct from available balance (for withdrawal)
     */
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount) {
        ViewerWallet wallet = viewerWalletRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.deductFromBalance(amount);
        viewerWalletRepository.save(wallet);
        logger.info("Deducted {} from balance for viewer {}", amount, userId);
    }

    /**
     * Check if user has sufficient balance
     */
    public boolean hasSufficientBalance(Long userId, BigDecimal amount) {
        ViewerWallet wallet = viewerWalletRepository.findByUserId(userId)
            .orElse(new ViewerWallet(userId));
        return wallet.getBalance().compareTo(amount) >= 0;
    }

    /**
     * Get available balance
     */
    public BigDecimal getAvailableBalance(Long userId) {
        ViewerWallet wallet = viewerWalletRepository.findByUserId(userId)
            .orElse(new ViewerWallet(userId));
        return wallet.getBalance();
    }
}

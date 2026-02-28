package com.growearn.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final CreatorWalletService creatorWalletService;
    private final ViewerWalletService viewerWalletService;

    public WalletService(CreatorWalletService creatorWalletService, ViewerWalletService viewerWalletService) {
        this.creatorWalletService = creatorWalletService;
        this.viewerWalletService = viewerWalletService;
    }

    public void creditBalance(Long userId, BigDecimal amount, String role) {
        String normalized = role != null ? role.toUpperCase() : "";
        if ("CREATOR".equals(normalized)) {
            creatorWalletService.creditBalance(userId, amount);
            return;
        }
        viewerWalletService.creditBalance(userId, amount);
    }

    public Object getWalletSnapshot(Long userId, String role) {
        String normalized = role != null ? role.toUpperCase() : "";
        if ("CREATOR".equals(normalized)) {
            var wallet = creatorWalletService.getOrCreateWallet(userId);
            return java.util.Map.of(
                "balance", wallet.getBalance(),
                "locked", wallet.getLockedBalance(),
                "available", wallet.getBalance().subtract(wallet.getLockedBalance())
            );
        }
        var wallet = viewerWalletService.getOrCreateWallet(userId);
        return java.util.Map.of(
            "balance", wallet.getBalance().add(wallet.getLockedBalance()),
            "locked", wallet.getLockedBalance(),
            "available", wallet.getBalance()
        );
    }
}

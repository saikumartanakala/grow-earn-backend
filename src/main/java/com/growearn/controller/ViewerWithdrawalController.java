package com.growearn.controller;

import com.growearn.dto.WalletDTO;
import com.growearn.dto.WithdrawalRequestDTO;
import com.growearn.dto.WithdrawalResponseDTO;
import com.growearn.entity.ViewerWallet;
import com.growearn.security.JwtUtil;
import com.growearn.service.ViewerWalletService;
import com.growearn.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/viewer")
public class ViewerWithdrawalController {

    private static final Logger logger = LoggerFactory.getLogger(ViewerWithdrawalController.class);

    private final ViewerWalletService viewerWalletService;
    private final WithdrawalService withdrawalService;
    private final JwtUtil jwtUtil;

    public ViewerWithdrawalController(
            ViewerWalletService viewerWalletService,
            WithdrawalService withdrawalService,
            JwtUtil jwtUtil) {
        this.viewerWalletService = viewerWalletService;
        this.withdrawalService = withdrawalService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get viewer wallet balance
     * GET /api/viewer/wallet
     */
    @GetMapping("/wallet")
    public ResponseEntity<Map<String, Object>> getWalletBalance(HttpServletRequest request) {
        try {
            Long viewerId = extractUserId(request);
            ViewerWallet wallet = viewerWalletService.getOrCreateWallet(viewerId);

            Map<String, Object> response = new HashMap<>();
            response.put("balance", wallet.getBalance().add(wallet.getLockedBalance()));
            response.put("available", wallet.getBalance());
            response.put("locked", wallet.getLockedBalance());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching wallet balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create withdrawal request
     * POST /api/viewer/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> createWithdrawal(
            @Valid @RequestBody WithdrawalRequestDTO requestDTO,
            HttpServletRequest request) {
        try {
            Long viewerId = extractUserId(request);
            
            WithdrawalResponseDTO response = withdrawalService.createWithdrawalRequest(viewerId, requestDTO);

            logger.info("Viewer {} created withdrawal request for ₹{}", viewerId, requestDTO.getAmount());

            Map<String, Object> result = new HashMap<>();
            result.put("id", response.getId());
            result.put("status", response.getStatus());
            result.put("message", "Withdrawal request submitted");

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.error("Error creating withdrawal request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating withdrawal request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create withdrawal request"));
        }
    }

    /**
     * Get viewer's withdrawal history
     * GET /api/viewer/withdrawals
     */
    @GetMapping("/withdrawals")
    public ResponseEntity<?> getWithdrawalHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long viewerId = extractUserId(request);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<WithdrawalResponseDTO> withdrawals = withdrawalService.getUserWithdrawals(viewerId, pageable);

            return ResponseEntity.ok(withdrawals.getContent());
        } catch (Exception e) {
            logger.error("Error fetching withdrawal history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Extract user ID from JWT token
     */
    private Long extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid authorization token");
        }
        return jwtUtil.extractUserId(auth.substring(7));
    }
}

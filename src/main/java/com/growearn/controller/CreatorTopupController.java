package com.growearn.controller;

import com.growearn.dto.CreatorTopupRequestDTO;
import com.growearn.dto.CreatorTopupResponseDTO;
import com.growearn.dto.WalletDTO;
import com.growearn.entity.CreatorWallet;
import com.growearn.security.JwtUtil;
import com.growearn.service.CreatorTopupService;
import com.growearn.service.CreatorWalletService;
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
@RequestMapping("/api/creator")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class CreatorTopupController {

    private static final Logger logger = LoggerFactory.getLogger(CreatorTopupController.class);

    private final CreatorWalletService creatorWalletService;
    private final CreatorTopupService creatorTopupService;
    private final JwtUtil jwtUtil;

    public CreatorTopupController(
            CreatorWalletService creatorWalletService,
            CreatorTopupService creatorTopupService,
            JwtUtil jwtUtil) {
        this.creatorWalletService = creatorWalletService;
        this.creatorTopupService = creatorTopupService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get creator wallet balance
     * GET /api/creator/wallet
     */
    @GetMapping("/wallet")
    public ResponseEntity<Map<String, Object>> getWalletBalance(HttpServletRequest request) {
        try {
            Long creatorId = extractUserId(request);
            CreatorWallet wallet = creatorWalletService.getOrCreateWallet(creatorId);
            java.math.BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("balance", wallet.getBalance());
            response.put("available", available);
            response.put("locked", wallet.getLockedBalance());
            response.put("lockedBalance", wallet.getLockedBalance());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching wallet balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create top-up request
     * POST /api/creator/topup
     */
    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> createTopup(
            @Valid @RequestBody CreatorTopupRequestDTO requestDTO,
            HttpServletRequest request) {
        try {
            Long creatorId = extractUserId(request);
            
            CreatorTopupResponseDTO response = creatorTopupService.createTopupRequest(creatorId, requestDTO);

            logger.info("Creator {} created top-up request for ₹{}", creatorId, requestDTO.getAmount());

            Map<String, Object> data = new HashMap<>();
            data.put("topupId", "top_" + response.getId());
            data.put("amount", response.getAmount());
            data.put("status", response.getStatus());
            data.put("upiRefId", response.getUpiReferenceId());
            data.put("proofUrl", response.getProofUrl());
            data.put("requestedAt", response.getCreatedAt());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Top-up request submitted successfully");
            result.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            logger.error("Error creating top-up request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating top-up request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create top-up request"));
        }
    }

    /**
     * Get creator's top-up history
     * GET /api/creator/topup/history
     */
    @GetMapping("/topup/history")
    public ResponseEntity<?> getTopupHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long creatorId = extractUserId(request);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<CreatorTopupResponseDTO> topups = creatorTopupService.getCreatorTopups(creatorId, pageable);

            return ResponseEntity.ok(topups.getContent());
        } catch (Exception e) {
            logger.error("Error fetching top-up history", e);
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

package com.growearn.controller;

import com.growearn.dto.ApprovalRequestDTO;
import com.growearn.dto.CreatorTopupResponseDTO;
import com.growearn.dto.WithdrawalResponseDTO;
import com.growearn.entity.TopupStatus;
import com.growearn.entity.WithdrawalStatus;
import com.growearn.security.JwtUtil;
import com.growearn.service.CreatorTopupService;
import com.growearn.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class AdminPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPaymentController.class);

    private final WithdrawalService withdrawalService;
    private final CreatorTopupService creatorTopupService;
    private final JwtUtil jwtUtil;

    public AdminPaymentController(
            WithdrawalService withdrawalService,
            CreatorTopupService creatorTopupService,
            JwtUtil jwtUtil) {
        this.withdrawalService = withdrawalService;
        this.creatorTopupService = creatorTopupService;
        this.jwtUtil = jwtUtil;
    }

    // ==================== WITHDRAWAL MANAGEMENT ====================

    /**
     * Get pending withdrawal requests
     * GET /api/admin/withdrawals/pending
     */
    @GetMapping("/withdrawals/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingWithdrawals() {
        try {
            List<WithdrawalResponseDTO> withdrawals = withdrawalService.getPendingWithdrawals();
            return ResponseEntity.ok(withdrawals);
        } catch (Exception e) {
            logger.error("Error fetching pending withdrawals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all withdrawal requests
     * GET /api/admin/withdrawals
     */
    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<WithdrawalResponseDTO> withdrawals;

            if (status != null && !status.isEmpty()) {
                WithdrawalStatus withdrawalStatus = WithdrawalStatus.valueOf(status.toUpperCase());
                withdrawals = withdrawalService.getWithdrawalsByStatus(withdrawalStatus, pageable);
            } else {
                withdrawals = withdrawalService.getAllWithdrawals(pageable);
            }

            return ResponseEntity.ok(withdrawals.getContent());
        } catch (Exception e) {
            logger.error("Error fetching withdrawals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve withdrawal request
     * POST /api/admin/withdrawals/approve
     */
    @PostMapping("/withdrawals/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveWithdrawal(
            @RequestBody Map<String, Long> payload,
            HttpServletRequest request) {
        try {
            Long adminId = extractUserId(request);
            Long withdrawalId = payload.get("withdrawalId");
            
            if (withdrawalId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "withdrawalId is required"));
            }
            
            withdrawalService.approveWithdrawal(withdrawalId, adminId);

            logger.info("Admin {} approved withdrawal request {}", adminId, withdrawalId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Withdrawal approved"
            ));
        } catch (RuntimeException e) {
            logger.error("Error approving withdrawal", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving withdrawal", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to approve withdrawal"));
        }
    }

    /**
     * Reject withdrawal request
     * POST /api/admin/withdrawals/reject
     */
    @PostMapping("/withdrawals/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rejectWithdrawal(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            Long adminId = extractUserId(request);
            Long withdrawalId = ((Number) payload.get("withdrawalId")).longValue();
            String reason = (String) payload.getOrDefault("reason", "No reason provided");
            
            if (withdrawalId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "withdrawalId is required"));
            }
            
            withdrawalService.rejectWithdrawal(withdrawalId, adminId, reason);

            logger.info("Admin {} rejected withdrawal request {}", adminId, withdrawalId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Withdrawal rejected"
            ));
        } catch (RuntimeException e) {
            logger.error("Error rejecting withdrawal", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting withdrawal", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reject withdrawal"));
        }
    }

    // ==================== CREATOR TOP-UP MANAGEMENT ====================

    /**
     * Get pending top-up requests
     * GET /api/admin/topups/pending
     */
    @GetMapping("/topups/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingTopups() {
        try {
            List<CreatorTopupResponseDTO> topups = creatorTopupService.getPendingTopups();
            return ResponseEntity.ok(topups);
        } catch (Exception e) {
            logger.error("Error fetching pending top-ups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all creator top-up requests
     * GET /api/admin/topups (alias for /topups/all)
     */
    @GetMapping("/topups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTopups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {
        return getAllTopups(status, page, limit);
    }

    /**
     * Get all creator top-up requests
     * GET /api/admin/topups/all
     */
    @GetMapping("/topups/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTopups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            // Convert page from 1-based to 0-based for Spring Data
            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<CreatorTopupResponseDTO> topupsPage;

            if (status != null && !status.isEmpty()) {
                TopupStatus topupStatus = TopupStatus.valueOf(status.toUpperCase());
                topupsPage = creatorTopupService.getTopupsByStatus(topupStatus, pageable);
            } else {
                topupsPage = creatorTopupService.getAllTopups(pageable);
            }

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", page);
            pagination.put("totalPages", topupsPage.getTotalPages());
            pagination.put("totalItems", topupsPage.getTotalElements());
            pagination.put("itemsPerPage", limit);

            Map<String, Object> response = new HashMap<>();
            response.put("topups", topupsPage.getContent());
            response.put("pagination", pagination);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching top-ups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve top-up request
     * POST /api/admin/topups/:topupId/approve
     */
    @PostMapping("/topups/{topupId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveTopup(
            @PathVariable Long topupId,
            HttpServletRequest request) {
        try {
            Long adminId = extractUserId(request);
            
            CreatorTopupResponseDTO approvedTopup = creatorTopupService.approveTopupWithBalance(topupId, adminId);

            logger.info("Admin {} approved top-up request {}", adminId, topupId);

            Map<String, Object> data = new HashMap<>();
            data.put("topupId", "top_" + approvedTopup.getId());
            data.put("amount", approvedTopup.getAmount());
            data.put("status", approvedTopup.getStatus());
            data.put("creatorNewBalance", approvedTopup.getCreatorNewBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Top-up approved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error approving top-up", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving top-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to approve top-up"));
        }
    }

    /**
     * Reject top-up request
     * POST /api/admin/topups/:topupId/reject
     */
    @PostMapping("/topups/{topupId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rejectTopup(
            @PathVariable Long topupId,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        try {
            Long adminId = extractUserId(request);
            String reason = payload.getOrDefault("reason", "No reason provided");
            
            CreatorTopupResponseDTO rejectedTopup = creatorTopupService.rejectTopup(topupId, adminId, reason);

            logger.info("Admin {} rejected top-up request {}", adminId, topupId);

            Map<String, Object> data = new HashMap<>();
            data.put("topupId", "top_" + rejectedTopup.getId());
            data.put("status", rejectedTopup.getStatus());
            data.put("reason", rejectedTopup.getRejectionReason());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Top-up rejected successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error rejecting top-up", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting top-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reject top-up"));
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

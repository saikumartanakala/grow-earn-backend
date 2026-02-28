package com.growearn.service;

import com.growearn.dto.WithdrawalRequestDTO;
import com.growearn.dto.WithdrawalResponseDTO;
import com.growearn.entity.User;
import com.growearn.entity.WithdrawalRequest;
import com.growearn.entity.WithdrawalStatus;
import com.growearn.repository.PayoutTransactionRepository;
import com.growearn.repository.UserRepository;
import com.growearn.repository.WithdrawalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WithdrawalService {

    private static final Logger logger = LoggerFactory.getLogger(WithdrawalService.class);
    private static final BigDecimal MINIMUM_WITHDRAWAL = new BigDecimal("10.00");

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ViewerWalletService viewerWalletService;
    private final UserRepository userRepository;
    private final UserViolationService userViolationService;
    private final PayoutTransactionRepository payoutTransactionRepository;

    public WithdrawalService(
            WithdrawalRequestRepository withdrawalRequestRepository,
            ViewerWalletService viewerWalletService,
            UserRepository userRepository,
            UserViolationService userViolationService,
            PayoutTransactionRepository payoutTransactionRepository) {
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.viewerWalletService = viewerWalletService;
        this.userRepository = userRepository;
        this.userViolationService = userViolationService;
        this.payoutTransactionRepository = payoutTransactionRepository;
    }

    /**
     * Create a withdrawal request
     */
    @Transactional
    public WithdrawalResponseDTO createWithdrawalRequest(Long userId, WithdrawalRequestDTO requestDTO) {
        // Validation: Check if user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validation: Check if user has violations
        if (userViolationService.hasActiveViolations(userId)) {
            throw new RuntimeException("Cannot withdraw: Account has active violations");
        }

        // Validation: Minimum withdrawal amount
        if (requestDTO.getAmount().compareTo(MINIMUM_WITHDRAWAL) < 0) {
            throw new RuntimeException("Minimum withdrawal amount is ₹" + MINIMUM_WITHDRAWAL);
        }

        // Validation: Check balance
        if (!viewerWalletService.hasSufficientBalance(userId, requestDTO.getAmount())) {
            throw new RuntimeException("Insufficient balance");
        }

        // Validation: Check for pending withdrawals
        long pendingCount = withdrawalRequestRepository.countPendingByUserId(userId);
        if (pendingCount > 0) {
            throw new RuntimeException("You already have a pending withdrawal request");
        }

        // Create withdrawal request
        WithdrawalRequest request = new WithdrawalRequest(
            userId,
            requestDTO.getAmount(),
            requestDTO.getUpiId()
        );

        WithdrawalRequest saved = withdrawalRequestRepository.save(request);
        logger.info("Created withdrawal request {} for user {} amount {}", saved.getId(), userId, requestDTO.getAmount());

        return mapToResponseDTO(saved);
    }

    /**
     * Get all withdrawal requests (admin)
     */
    public Page<WithdrawalResponseDTO> getAllWithdrawals(Pageable pageable) {
        return withdrawalRequestRepository.findAll(pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get withdrawal requests by status (admin)
     */
    public Page<WithdrawalResponseDTO> getWithdrawalsByStatus(WithdrawalStatus status, Pageable pageable) {
        return withdrawalRequestRepository.findByStatus(status, pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get user's withdrawal history
     */
    public Page<WithdrawalResponseDTO> getUserWithdrawals(Long userId, Pageable pageable) {
        return withdrawalRequestRepository.findByUserId(userId, pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get pending withdrawals ordered by date
     */
    public List<WithdrawalResponseDTO> getPendingWithdrawals() {
        return withdrawalRequestRepository.findPendingWithdrawalsOrderByDate(WithdrawalStatus.PENDING)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Approve withdrawal request (admin)
     */
    @Transactional
    public WithdrawalResponseDTO approveWithdrawal(Long requestId, Long adminId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        if (!request.isPending()) {
            throw new RuntimeException("Withdrawal request is not pending");
        }

        // Verify user still has sufficient balance
        if (!viewerWalletService.hasSufficientBalance(request.getUserId(), request.getAmount())) {
            throw new RuntimeException("User has insufficient balance for this withdrawal");
        }

        // Deduct balance
        viewerWalletService.deductBalance(request.getUserId(), request.getAmount());

        // Approve request
        request.approve(adminId);
        WithdrawalRequest updated = withdrawalRequestRepository.save(request);

        logger.info("Admin {} approved withdrawal request {} for user {} amount {}", 
            adminId, requestId, request.getUserId(), request.getAmount());

        return mapToResponseDTO(updated);
    }

    /**
     * Reject withdrawal request (admin)
     */
    @Transactional
    public WithdrawalResponseDTO rejectWithdrawal(Long requestId, Long adminId, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        if (!request.isPending()) {
            throw new RuntimeException("Withdrawal request is not pending");
        }

        // Reject request
        request.reject(adminId, reason);
        WithdrawalRequest updated = withdrawalRequestRepository.save(request);

        logger.info("Admin {} rejected withdrawal request {} for user {}: {}", 
            adminId, requestId, request.getUserId(), reason);

        return mapToResponseDTO(updated);
    }

    /**
     * Prepare withdrawal for RazorpayX payout (admin)
     */
    @Transactional
    public WithdrawalRequest preparePayout(Long requestId, Long adminId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        if (!request.isPending()) {
            throw new RuntimeException("Withdrawal request is not pending");
        }

        if (!viewerWalletService.hasSufficientBalance(request.getUserId(), request.getAmount())) {
            throw new RuntimeException("User has insufficient balance for this withdrawal");
        }

        viewerWalletService.deductBalance(request.getUserId(), request.getAmount());
        request.markProcessing(adminId);
        return withdrawalRequestRepository.save(request);
    }

    /**
     * Map entity to DTO
     */
    private WithdrawalResponseDTO mapToResponseDTO(WithdrawalRequest request) {
        WithdrawalResponseDTO dto = new WithdrawalResponseDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUserId());
        dto.setAmount(request.getAmount());
        dto.setUpiId(request.getUpiId());
        dto.setStatus(request.getStatus().name());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setCreatedAt(request.getRequestedAt());
        dto.setProcessedAt(request.getProcessedAt());
        dto.setProcessedBy(request.getProcessedBy());
        dto.setRejectionReason(request.getRejectionReason());
        dto.setReason(request.getRejectionReason());

        String payoutStatus = payoutTransactionRepository.findByWithdrawalId(request.getId())
            .map(payout -> normalizePayoutStatus(payout.getStatus()))
            .orElseGet(() -> {
                if (request.getStatus() == WithdrawalStatus.PROCESSING) return "PROCESSING";
                if (request.getStatus() == WithdrawalStatus.FAILED) return "FAILED";
                if (request.getStatus() == WithdrawalStatus.PAID) return "SUCCESS";
                return "PENDING";
            });
        dto.setPayoutStatus(payoutStatus);

        // Set user email and userName
        if (request.getUser() != null) {
            dto.setUserEmail(request.getUser().getEmail());
            dto.setUserName(request.getUser().getEmail());
        }

        // Set processor email
        if (request.getProcessor() != null) {
            dto.setProcessedByEmail(request.getProcessor().getEmail());
        }

        return dto;
    }

    private String normalizePayoutStatus(String status) {
        if (status == null) return "PENDING";
        String normalized = status.trim().toUpperCase();
        if ("PROCESSED".equals(normalized)) return "SUCCESS";
        if ("PROCESSING".equals(normalized)) return "PROCESSING";
        if ("FAILED".equals(normalized)) return "FAILED";
        if ("SUCCESS".equals(normalized)) return "SUCCESS";
        return normalized;
    }
}

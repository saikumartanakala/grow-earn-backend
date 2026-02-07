package com.growearn.service;

import com.growearn.dto.CreatorTopupRequestDTO;
import com.growearn.dto.CreatorTopupResponseDTO;
import com.growearn.entity.CreatorTopup;
import com.growearn.entity.Role;
import com.growearn.entity.TopupStatus;
import com.growearn.entity.User;
import com.growearn.repository.CreatorTopupRepository;
import com.growearn.repository.UserRepository;
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
public class CreatorTopupService {

    private static final Logger logger = LoggerFactory.getLogger(CreatorTopupService.class);
    private static final BigDecimal MINIMUM_TOPUP = new BigDecimal("50.00");

    private final CreatorTopupRepository creatorTopupRepository;
    private final CreatorWalletService creatorWalletService;
    private final UserRepository userRepository;
    private final UserViolationService userViolationService;

    public CreatorTopupService(
            CreatorTopupRepository creatorTopupRepository,
            CreatorWalletService creatorWalletService,
            UserRepository userRepository,
            UserViolationService userViolationService) {
        this.creatorTopupRepository = creatorTopupRepository;
        this.creatorWalletService = creatorWalletService;
        this.userRepository = userRepository;
        this.userViolationService = userViolationService;
    }

    /**
     * Create a top-up request
     */
    @Transactional
    public CreatorTopupResponseDTO createTopupRequest(Long creatorId, CreatorTopupRequestDTO requestDTO) {
        // Validation: Check if user exists and is a creator
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("Creator not found"));

        if (creator.getRole() != Role.CREATOR) {
            throw new RuntimeException("Only creators can add funds");
        }

        // Validation: Check if user has violations
        if (userViolationService.hasActiveViolations(creatorId)) {
            throw new RuntimeException("Cannot add funds: Account has active violations");
        }

        // Validation: Minimum top-up amount
        if (requestDTO.getAmount().compareTo(MINIMUM_TOPUP) < 0) {
            throw new RuntimeException("Minimum top-up amount is ₹" + MINIMUM_TOPUP);
        }

        // Create top-up request
        CreatorTopup topup = new CreatorTopup(
            creatorId,
            requestDTO.getAmount(),
            requestDTO.getUpiReferenceId(),
            requestDTO.getProofUrl()
        );

        CreatorTopup saved = creatorTopupRepository.save(topup);
        logger.info("Created top-up request {} for creator {} amount {}", 
            saved.getId(), creatorId, requestDTO.getAmount());

        return mapToResponseDTO(saved);
    }

    /**
     * Get all top-up requests (admin)
     */
    public Page<CreatorTopupResponseDTO> getAllTopups(Pageable pageable) {
        return creatorTopupRepository.findAll(pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get top-up requests by status (admin)
     */
    public Page<CreatorTopupResponseDTO> getTopupsByStatus(TopupStatus status, Pageable pageable) {
        return creatorTopupRepository.findByStatus(status, pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get creator's top-up history
     */
    public Page<CreatorTopupResponseDTO> getCreatorTopups(Long creatorId, Pageable pageable) {
        return creatorTopupRepository.findByCreatorId(creatorId, pageable)
            .map(this::mapToResponseDTO);
    }

    /**
     * Get pending top-ups ordered by date
     */
    public List<CreatorTopupResponseDTO> getPendingTopups() {
        return creatorTopupRepository.findPendingTopupsOrderByDate(TopupStatus.PENDING)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Approve top-up request (admin) - returns updated balance
     */
    @Transactional
    public CreatorTopupResponseDTO approveTopupWithBalance(Long topupId, Long adminId) {
        CreatorTopup topup = creatorTopupRepository.findById(topupId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        if (!topup.isPending()) {
            throw new RuntimeException("Top-up request is not pending");
        }

        // Credit creator wallet
        creatorWalletService.creditBalance(topup.getCreatorId(), topup.getAmount());

        // Approve request
        topup.approve(adminId);
        CreatorTopup updated = creatorTopupRepository.save(topup);

        logger.info("Admin {} approved top-up request {} for creator {} amount {}", 
            adminId, topupId, topup.getCreatorId(), topup.getAmount());

        // Get updated balance
        CreatorTopupResponseDTO dto = mapToResponseDTO(updated);
        dto.setCreatorNewBalance(creatorWalletService.getWallet(topup.getCreatorId()).getBalance());

        return dto;
    }

    /**
     * Approve top-up request (admin)
     */
    @Transactional
    public CreatorTopupResponseDTO approveTopup(Long topupId, Long adminId) {
        CreatorTopup topup = creatorTopupRepository.findById(topupId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        if (!topup.isPending()) {
            throw new RuntimeException("Top-up request is not pending");
        }

        // Credit creator wallet
        creatorWalletService.creditBalance(topup.getCreatorId(), topup.getAmount());

        // Approve request
        topup.approve(adminId);
        CreatorTopup updated = creatorTopupRepository.save(topup);

        logger.info("Admin {} approved top-up request {} for creator {} amount {}", 
            adminId, topupId, topup.getCreatorId(), topup.getAmount());

        return mapToResponseDTO(updated);
    }

    /**
     * Reject top-up request (admin)
     */
    @Transactional
    public CreatorTopupResponseDTO rejectTopup(Long topupId, Long adminId, String reason) {
        CreatorTopup topup = creatorTopupRepository.findById(topupId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        if (!topup.isPending()) {
            throw new RuntimeException("Top-up request is not pending");
        }

        // Reject request
        topup.reject(adminId, reason);
        CreatorTopup updated = creatorTopupRepository.save(topup);

        logger.info("Admin {} rejected top-up request {} for creator {}: {}", 
            adminId, topupId, topup.getCreatorId(), reason);

        return mapToResponseDTO(updated);
    }

    /**
     * Map entity to DTO
     */
    private CreatorTopupResponseDTO mapToResponseDTO(CreatorTopup topup) {
        CreatorTopupResponseDTO dto = new CreatorTopupResponseDTO();
        dto.setId(topup.getId());
        dto.setCreatorId(topup.getCreatorId());
        dto.setUserId(topup.getCreatorId());
        dto.setAmount(topup.getAmount());
        dto.setUpiReference(topup.getUpiReference());
        dto.setUpiReferenceId(topup.getUpiReference());
        dto.setProofUrl(topup.getProofUrl());
        dto.setStatus(topup.getStatus().name());
        dto.setCreatedAt(topup.getCreatedAt());
        dto.setApprovedAt(topup.getApprovedAt());
        dto.setApprovedBy(topup.getApprovedBy());
        dto.setRejectionReason(topup.getRejectionReason());
        dto.setReason(topup.getRejectionReason());

        // Set creator email and userName
        if (topup.getCreator() != null) {
            dto.setCreatorEmail(topup.getCreator().getEmail());
            if (topup.getCreator().getFullName() != null) {
                dto.setCreatorName(topup.getCreator().getFullName());
                dto.setUserName(topup.getCreator().getFullName());
            } else {
                dto.setUserName(topup.getCreator().getEmail());
            }
        }

        // Set approver email
        if (topup.getApprover() != null) {
            dto.setApprovedByEmail(topup.getApprover().getEmail());
        }

        return dto;
    }
}

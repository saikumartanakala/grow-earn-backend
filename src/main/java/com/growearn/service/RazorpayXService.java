package com.growearn.service;

import com.growearn.entity.PayoutTransaction;
import com.growearn.entity.WithdrawalRequest;
import com.growearn.repository.PayoutTransactionRepository;
import com.growearn.repository.WithdrawalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class RazorpayXService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayXService.class);

    @Value("${razorpayx.key:}")
    private String razorpayXKey;

    @Value("${razorpayx.secret:}")
    private String razorpayXSecret;

    @Value("${razorpayx.account-number:}")
    private String razorpayXAccountNumber;

    private final RestTemplate restTemplate = new RestTemplate();
    private final PayoutTransactionRepository payoutTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    public RazorpayXService(PayoutTransactionRepository payoutTransactionRepository,
                            WithdrawalRequestRepository withdrawalRequestRepository) {
        this.payoutTransactionRepository = payoutTransactionRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
    }

    public PayoutTransaction createPayout(WithdrawalRequest request, Long adminId) {
        if (razorpayXKey.isBlank() || razorpayXSecret.isBlank() || razorpayXAccountNumber.isBlank()) {
            throw new IllegalStateException("RazorpayX credentials not configured");
        }

        long paise = request.getAmount().multiply(new BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = razorpayXKey + ":" + razorpayXSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> payload = Map.of(
            "account_number", razorpayXAccountNumber,
            "amount", paise,
            "currency", "INR",
            "mode", "UPI",
            "purpose", "payout",
            "fund_account", Map.of(
                "account_type", "vpa",
                "vpa", Map.of("address", request.getUpiId())
            ),
            "narration", "GrowEarn Payout"
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.razorpay.com/v1/payouts", HttpMethod.POST, httpEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to create RazorpayX payout");
        }

        Map<String, Object> body = response.getBody();
        String payoutId = String.valueOf(body.get("id"));
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : "processing";
        String normalizedStatus = normalizeStatus(status);

        PayoutTransaction payout = new PayoutTransaction();
        payout.setWithdrawalId(request.getId());
        payout.setUserId(request.getUserId());
        payout.setAmount(request.getAmount());
        payout.setCurrency("INR");
        payout.setMode("UPI");
        payout.setPurpose("payout");
        payout.setFundAccount(request.getUpiId());
        payout.setRazorpayPayoutId(payoutId);
        payout.setStatus(normalizedStatus);
        return payoutTransactionRepository.save(payout);
    }

    public void handleWebhook(Map<String, Object> payload) {
        Map<String, Object> entity = extractPayoutEntity(payload);
        if (entity == null) {
            logger.warn("RazorpayX webhook missing payout entity");
            return;
        }

        String payoutId = String.valueOf(entity.get("id"));
        String status = entity.get("status") != null ? String.valueOf(entity.get("status")) : "";
        String normalizedStatus = normalizeStatus(status);
        String failureReason = entity.get("failure_reason") != null ? String.valueOf(entity.get("failure_reason")) : null;

        Optional<PayoutTransaction> payoutOpt = payoutTransactionRepository.findByRazorpayPayoutId(payoutId);
        if (payoutOpt.isEmpty()) {
            logger.warn("No payout transaction found for payout {}", payoutId);
            return;
        }

        PayoutTransaction payout = payoutOpt.get();
        payout.setStatus(normalizedStatus);
        payout.setProcessedAt(LocalDateTime.now());
        payout.setFailureReason(failureReason);
        payoutTransactionRepository.save(payout);

        withdrawalRequestRepository.findById(payout.getWithdrawalId()).ifPresent(withdrawal -> {
            if ("SUCCESS".equalsIgnoreCase(normalizedStatus)) {
                withdrawal.markPaid();
            } else if ("FAILED".equalsIgnoreCase(normalizedStatus)) {
                withdrawal.markFailed(failureReason != null ? failureReason : "Payout failed");
            }
            withdrawalRequestRepository.save(withdrawal);
        });
    }

    private Map<String, Object> extractPayoutEntity(Map<String, Object> payload) {
        Object payloadObj = payload.get("payload");
        if (!(payloadObj instanceof Map<?, ?> payloadMap)) {
            return null;
        }
        Object payoutObj = payloadMap.get("payout");
        if (!(payoutObj instanceof Map<?, ?> payoutMap)) {
            return null;
        }
        Object entityObj = payoutMap.get("entity");
        if (!(entityObj instanceof Map<?, ?> entityMap)) {
            return null;
        }
        return (Map<String, Object>) entityMap;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "PENDING";
        String normalized = status.trim().toUpperCase();
        if ("PROCESSED".equals(normalized)) return "SUCCESS";
        if ("PROCESSING".equals(normalized)) return "PROCESSING";
        if ("FAILED".equals(normalized)) return "FAILED";
        return normalized;
    }
}

package com.growearn.controller;

import com.growearn.entity.PaymentTransaction;
import com.growearn.entity.CreatorTopup;
import com.growearn.entity.TopupStatus;
import com.growearn.repository.PaymentTransactionRepository;
import com.growearn.repository.CreatorTopupRepository;
import com.growearn.security.JwtUtil;
import com.growearn.service.RazorpayService;
import com.growearn.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class RazorpayController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayService razorpayService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CreatorTopupRepository creatorTopupRepository;
    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    @Value("${razorpay.key}")
    private String razorpayKey;


    public RazorpayController(RazorpayService razorpayService,
                              PaymentTransactionRepository paymentTransactionRepository,
                              CreatorTopupRepository creatorTopupRepository,
                              WalletService walletService,
                              JwtUtil jwtUtil) {
        this.razorpayService = razorpayService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.creatorTopupRepository = creatorTopupRepository;
        this.walletService = walletService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            if (body.get("amount") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "amount is required"));
            }
            BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "amount must be greater than 0"));
            }

            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid authorization token"));
            }
            String token = auth.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            String receipt = "wallet_" + userId + "_" + System.currentTimeMillis();
            Map<String, Object> order = razorpayService.createOrder(amount, receipt);
            String orderId = String.valueOf(order.get("id"));

            PaymentTransaction tx = new PaymentTransaction();
            tx.setUserId(userId);
            tx.setUserRole(role);
            tx.setAmount(amount);
            tx.setCurrency("INR");
            tx.setRazorpayOrderId(orderId);
            tx.setStatus("CREATED");
            paymentTransactionRepository.save(tx);

            long paise = amount.multiply(new java.math.BigDecimal("100"))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();

            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "amount", paise,
                "currency", "INR",
                "keyId", razorpayKey,
                "notes", Map.of("userId", String.valueOf(userId))
            ));
        } catch (Exception e) {
            logger.error("Error creating Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create order"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            String orderId = body.get("orderId") != null ? String.valueOf(body.get("orderId"))
                : body.get("order_id") != null ? String.valueOf(body.get("order_id"))
                : String.valueOf(body.get("razorpay_order_id"));
            String paymentId = body.get("paymentId") != null ? String.valueOf(body.get("paymentId"))
                : body.get("payment_id") != null ? String.valueOf(body.get("payment_id"))
                : String.valueOf(body.get("razorpay_payment_id"));
            String signature = body.get("signature") != null ? String.valueOf(body.get("signature"))
                : String.valueOf(body.get("razorpay_signature"));

            if (orderId == null || "null".equals(orderId) || paymentId == null || "null".equals(paymentId)
                || signature == null || "null".equals(signature)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orderId, paymentId, and signature are required"));
            }

            Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByRazorpayOrderId(orderId);
            if (txOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
            }
            PaymentTransaction tx = txOpt.get();
            if (Boolean.TRUE.equals(tx.getCredited())) {
                Object wallet = walletService.getWalletSnapshot(tx.getUserId(), tx.getUserRole());
                return ResponseEntity.ok(Map.of("success", true, "message", "Already credited", "wallet", wallet));
            }

            boolean valid = razorpayService.verifySignature(orderId, paymentId, signature);
            if (!valid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Invalid signature"));
            }

            tx.setRazorpayPaymentId(paymentId);
            tx.setSignature(signature);
            tx.setVerifiedAt(LocalDateTime.now());
            tx.setStatus("VERIFIED");
            paymentTransactionRepository.save(tx);

            walletService.creditBalance(tx.getUserId(), tx.getAmount(), tx.getUserRole());
            tx.setCredited(true);
            tx.setCreditedAt(LocalDateTime.now());
            tx.setStatus("PAID");
            paymentTransactionRepository.save(tx);

            Object wallet = walletService.getWalletSnapshot(tx.getUserId(), tx.getUserRole());
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("verified", true);
            response.put("message", "Payment verified");
            response.put("wallet", wallet);
            Map<String, Object> topup = createCreatorTopupIfNeeded(tx, paymentId, orderId);
            if (topup != null) {
                response.put("topup", topup);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error verifying payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to verify payment"));
        }
    }


    private Map<String, Object> createCreatorTopupIfNeeded(PaymentTransaction tx, String paymentId, String orderId) {
        String role = tx.getUserRole() != null ? tx.getUserRole().toUpperCase() : "";
        if (!role.contains("CREATOR")) {
            return null;
        }
        CreatorTopup topup = new CreatorTopup();
        topup.setCreatorId(tx.getUserId());
        topup.setAmount(tx.getAmount());
        topup.setUpiReference("razorpay:" + paymentId);
        topup.setStatus(TopupStatus.APPROVED);
        topup.setApprovedAt(LocalDateTime.now());
        topup.setApprovedBy(null);
        CreatorTopup saved = creatorTopupRepository.save(topup);
        return Map.of(
            "id", saved.getId(),
            "amount", saved.getAmount(),
            "status", saved.getStatus().name(),
            "upiReference", saved.getUpiReference(),
            "orderId", orderId,
            "createdAt", saved.getCreatedAt(),
            "approvedAt", saved.getApprovedAt()
        );
    }
}

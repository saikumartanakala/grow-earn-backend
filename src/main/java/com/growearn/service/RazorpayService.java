package com.growearn.service;

import com.growearn.entity.PaymentTransaction;
import com.growearn.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;


    private final RestTemplate restTemplate = new RestTemplate();
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletService walletService;

    public RazorpayService(PaymentTransactionRepository paymentTransactionRepository, WalletService walletService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.walletService = walletService;
    }

    public Map<String, Object> createOrder(BigDecimal amount, String receipt) {
        if (razorpayKey == null || razorpayKey.isBlank() || razorpaySecret == null || razorpaySecret.isBlank()) {
            throw new IllegalStateException("Razorpay credentials not configured");
        }

        long paise = amount.multiply(new BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = razorpayKey + ":" + razorpaySecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> payload = Map.of(
            "amount", paise,
            "currency", "INR",
            "receipt", receipt
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.razorpay.com/v1/orders", HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to create Razorpay order");
        }

        return response.getBody();
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (razorpaySecret == null || razorpaySecret.isBlank()) {
            throw new IllegalStateException("Razorpay secret not configured");
        }
        String payload = orderId + "|" + paymentId;
        String expected = hmacSha256(payload, razorpaySecret);
        return expected.equals(signature);
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            return false;
        }
        String expected = hmacSha256(payload, razorpayWebhookSecret);
        return expected.equals(signature);
    }

    public void handlePaymentCaptured(Map<String, Object> payload) {
        Map<String, Object> payment = extractPaymentEntity(payload);
        if (payment == null) {
            logger.warn("Razorpay webhook missing payment entity");
            return;
        }

        String paymentId = String.valueOf(payment.get("id"));
        String orderId = payment.containsKey("order_id") ? String.valueOf(payment.get("order_id")) : null;

        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByRazorpayPaymentId(paymentId);
        if (txOpt.isEmpty() && orderId != null) {
            txOpt = paymentTransactionRepository.findByRazorpayOrderId(orderId);
        }
        if (txOpt.isEmpty()) {
            logger.warn("No payment transaction found for Razorpay payment {}", paymentId);
            return;
        }

        PaymentTransaction tx = txOpt.get();
        if (tx.getRazorpayPaymentId() == null || tx.getRazorpayPaymentId().isBlank()) {
            tx.setRazorpayPaymentId(paymentId);
        }
        if (Boolean.TRUE.equals(tx.getCredited())) {
            tx.setStatus("CAPTURED");
            paymentTransactionRepository.save(tx);
            return;
        }

        walletService.creditBalance(tx.getUserId(), tx.getAmount(), tx.getUserRole());
        tx.setCredited(true);
        tx.setCreditedAt(LocalDateTime.now());
        tx.setStatus("CAPTURED");
        paymentTransactionRepository.save(tx);
    }

    private Map<String, Object> extractPaymentEntity(Map<String, Object> payload) {
        Object payloadObj = payload.get("payload");
        if (!(payloadObj instanceof Map<?, ?> payloadMap)) {
            return null;
        }
        Object paymentObj = payloadMap.get("payment");
        if (!(paymentObj instanceof Map<?, ?> paymentMap)) {
            return null;
        }
        Object entityObj = paymentMap.get("entity");
        if (!(entityObj instanceof Map<?, ?> entityMap)) {
            return null;
        }
        return (Map<String, Object>) entityMap;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] signed = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : signed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }
}

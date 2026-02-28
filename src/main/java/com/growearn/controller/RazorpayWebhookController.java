package com.growearn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growearn.service.RazorpayService;
import com.growearn.service.RazorpayXService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class RazorpayWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayService razorpayService;
    private final RazorpayXService razorpayXService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayWebhookController(RazorpayService razorpayService, RazorpayXService razorpayXService) {
        this.razorpayService = razorpayService;
        this.razorpayXService = razorpayXService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload, HttpServletRequest request) {
        try {
            String signature = request.getHeader("X-Razorpay-Signature");
            if (signature == null || !razorpayService.verifyWebhookSignature(payload, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
            }

            Map<String, Object> body = objectMapper.readValue(payload, Map.class);
            String event = body.get("event") != null ? String.valueOf(body.get("event")) : "";

            if ("payment.captured".equalsIgnoreCase(event)) {
                razorpayService.handlePaymentCaptured(body);
            } else if ("payout.processed".equalsIgnoreCase(event) || "payout.failed".equalsIgnoreCase(event)) {
                razorpayXService.handleWebhook(body);
            } else {
                logger.info("Unhandled Razorpay webhook event: {}", event);
            }

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Error handling Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Webhook processing failed"));
        }
    }
}

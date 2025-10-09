package com.creditcontrol.payment.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class PaymentController {

    @GetMapping("/history/{customerId}")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(@PathVariable Long customerId) {
        log.info("API_REQUEST: GET /api/v1/payments/history/{}", customerId);
        
        List<Map<String, Object>> payments = Arrays.asList(
            Map.of("paymentId", 1, "amount", new BigDecimal("25000.00"), "date", "2025-10-01", "status", "COMPLETED", "method", "BANK_TRANSFER"),
            Map.of("paymentId", 2, "amount", new BigDecimal("15000.00"), "date", "2025-09-15", "status", "COMPLETED", "method", "CHECK"),
            Map.of("paymentId", 3, "amount", new BigDecimal("30000.00"), "date", "2025-08-30", "status", "COMPLETED", "method", "WIRE_TRANSFER")
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "customerId", customerId,
            "payments", payments,
            "totalPaid", new BigDecimal("70000.00"),
            "averagePayment", new BigDecimal("23333.33"),
            "onTimePayments", 3,
            "latePayments", 0
        ));
        response.put("message", "Payment history retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paymentRequest) {
        log.info("API_REQUEST: POST /api/v1/payments/process");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "paymentId", new Random().nextInt(10000),
            "status", "PROCESSING",
            "amount", paymentRequest.get("amount"),
            "customerId", paymentRequest.get("customerId"),
            "method", paymentRequest.get("method"),
            "transactionId", "TXN" + System.currentTimeMillis(),
            "estimatedCompletion", "2025-10-10T10:00:00Z"
        ));
        response.put("message", "Payment processing initiated");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPaymentSummary() {
        log.info("API_REQUEST: GET /api/v1/payments/summary");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "totalPaymentsToday", 15,
            "totalAmountToday", new BigDecimal("450000.00"),
            "totalPaymentsMonth", 234,
            "totalAmountMonth", new BigDecimal("5600000.00"),
            "pendingPayments", 8,
            "failedPayments", 2,
            "averageProcessingTime", "2.5 hours"
        ));
        response.put("message", "Payment summary retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "payment-service");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "payment-service");
        response.put("description", "Payment Processing and Tracking Service");
        response.put("version", "1.0.0");
        response.put("endpoints", Arrays.asList(
            "GET /api/v1/payments/history/{customerId}",
            "POST /api/v1/payments/process",
            "GET /api/v1/payments/summary"
        ));
        return ResponseEntity.ok(response);
    }
}
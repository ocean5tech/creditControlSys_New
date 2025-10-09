package com.creditcontrol.credit.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Credit Service REST Controller
 * 信用管理服务API控制器
 */
@RestController
@RequestMapping("/api/v1/credit")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class CreditController {

    @GetMapping("/profile/{customerId}")
    public ResponseEntity<Map<String, Object>> getCreditProfile(@PathVariable Long customerId) {
        log.info("API_REQUEST: GET /api/v1/credit/profile/{}", customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "customerId", customerId,
            "creditLimit", new BigDecimal("500000.00"),
            "availableCredit", new BigDecimal("350000.00"),
            "usedCredit", new BigDecimal("150000.00"),
            "creditRating", "A",
            "riskScore", 85,
            "utilization", 30.0,
            "lastReviewDate", "2025-10-01",
            "nextReviewDate", "2026-01-01"
        ));
        response.put("message", "Credit profile retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessment/{customerId}")
    public ResponseEntity<Map<String, Object>> assessCreditRisk(@PathVariable Long customerId) {
        log.info("API_REQUEST: GET /api/v1/credit/assessment/{}", customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "customerId", customerId,
            "currentRiskScore", 85,
            "newRiskScore", 88,
            "riskLevel", "LOW",
            "recommendedCreditLimit", new BigDecimal("600000.00"),
            "riskFactors", Arrays.asList(
                "Excellent payment history", 
                "Stable industry", 
                "Good transaction volume"
            ),
            "assessmentDate", Instant.now().toString()
        ));
        response.put("message", "Credit risk assessment completed");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getCreditSummary() {
        log.info("API_REQUEST: GET /api/v1/credit/summary");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "totalCreditLimit", new BigDecimal("2500000.00"),
            "totalUsedCredit", new BigDecimal("850000.00"),
            "totalAvailableCredit", new BigDecimal("1650000.00"),
            "averageUtilization", 34.0,
            "totalCustomers", 5,
            "highRiskCustomers", 1,
            "mediumRiskCustomers", 2,
            "lowRiskCustomers", 2
        ));
        response.put("message", "Credit summary retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "credit-service");
        response.put("timestamp", Instant.now().toString());
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "credit-service");
        response.put("description", "Credit Management and Risk Assessment Service");
        response.put("version", "1.0.0");
        response.put("endpoints", Arrays.asList(
            "GET /api/v1/credit/profile/{customerId}",
            "GET /api/v1/credit/assessment/{customerId}",
            "GET /api/v1/credit/summary",
            "GET /api/v1/credit/health",
            "GET /api/v1/credit/info"
        ));
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
package com.creditcontrol.risk.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/risk")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class RiskController {

    @GetMapping("/assessment/{customerId}")
    public ResponseEntity<Map<String, Object>> getRiskAssessment(@PathVariable Long customerId) {
        log.info("API_REQUEST: GET /api/v1/risk/assessment/{}", customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "customerId", customerId,
            "riskScore", 85,
            "riskLevel", "LOW",
            "riskFactors", Arrays.asList("Payment history: Excellent", "Industry: Stable", "Transaction volume: Good"),
            "recommendations", Arrays.asList("Maintain current credit terms", "Monitor monthly transactions"),
            "lastAssessment", "2025-10-09",
            "nextAssessment", "2025-11-09"
        ));
        response.put("message", "Risk assessment retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monitoring/dashboard")
    public ResponseEntity<Map<String, Object>> getRiskDashboard() {
        log.info("API_REQUEST: GET /api/v1/risk/monitoring/dashboard");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "totalCustomers", 5,
            "highRiskCount", 1,
            "mediumRiskCount", 2,
            "lowRiskCount", 2,
            "avgRiskScore", 78.5,
            "alertsCount", 3,
            "trendsDirection", "IMPROVING"
        ));
        response.put("message", "Risk monitoring dashboard data retrieved");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "risk-service");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "risk-service");
        response.put("description", "Risk Assessment and Monitoring Service");
        response.put("version", "1.0.0");
        response.put("endpoints", Arrays.asList(
            "GET /api/v1/risk/assessment/{customerId}",
            "GET /api/v1/risk/monitoring/dashboard"
        ));
        return ResponseEntity.ok(response);
    }
}
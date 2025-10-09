package com.creditcontrol.notification.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class NotificationController {

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        log.info("API_REQUEST: GET /api/v1/notifications/alerts");
        
        List<Map<String, Object>> alerts = Arrays.asList(
            Map.of(
                "alertId", 1,
                "type", "CREDIT_LIMIT_EXCEEDED",
                "customerId", 2,
                "customerName", "XYZ Trading Corp",
                "message", "Credit utilization exceeded 90%",
                "severity", "HIGH",
                "timestamp", "2025-10-09T07:30:00Z",
                "status", "ACTIVE"
            ),
            Map.of(
                "alertId", 2,
                "type", "PAYMENT_OVERDUE",
                "customerId", 4,
                "customerName", "Tech Solutions LLC",
                "message", "Payment overdue by 15 days",
                "severity", "MEDIUM",
                "timestamp", "2025-10-09T06:15:00Z",
                "status", "ACTIVE"
            ),
            Map.of(
                "alertId", 3,
                "type", "RISK_SCORE_CHANGE",
                "customerId", 1,
                "customerName", "ABC Manufacturing Ltd",
                "message", "Risk score improved from 80 to 85",
                "severity", "LOW",
                "timestamp", "2025-10-09T05:00:00Z",
                "status", "ACKNOWLEDGED"
            )
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "alerts", alerts,
            "totalAlerts", 3,
            "activeAlerts", 2,
            "highSeverityAlerts", 1
        ));
        response.put("message", "Alerts retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> notificationRequest) {
        log.info("API_REQUEST: POST /api/v1/notifications/send");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "notificationId", "NOT_" + System.currentTimeMillis(),
            "status", "SENT",
            "recipient", notificationRequest.get("recipient"),
            "channel", notificationRequest.get("channel"),
            "message", notificationRequest.get("message"),
            "sentAt", Instant.now().toString()
        ));
        response.put("message", "Notification sent successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{customerId}")
    public ResponseEntity<Map<String, Object>> getNotificationHistory(@PathVariable Long customerId) {
        log.info("API_REQUEST: GET /api/v1/notifications/history/{}", customerId);
        
        List<Map<String, Object>> history = Arrays.asList(
            Map.of(
                "notificationId", "NOT_1728123456789",
                "type", "PAYMENT_REMINDER",
                "message", "Payment due in 3 days",
                "channel", "EMAIL",
                "status", "DELIVERED",
                "sentAt", "2025-10-06T10:00:00Z"
            ),
            Map.of(
                "notificationId", "NOT_1728023456789",
                "type", "CREDIT_LIMIT_UPDATE",
                "message", "Your credit limit has been increased to $500,000",
                "channel", "SMS",
                "status", "DELIVERED",
                "sentAt", "2025-10-01T14:30:00Z"
            )
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "customerId", customerId,
            "notifications", history,
            "totalSent", 2,
            "deliveryRate", 100.0
        ));
        response.put("message", "Notification history retrieved successfully");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "notification-service");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "notification-service");
        response.put("description", "Alert and Communication Service");
        response.put("version", "1.0.0");
        response.put("endpoints", Arrays.asList(
            "GET /api/v1/notifications/alerts",
            "POST /api/v1/notifications/send",
            "GET /api/v1/notifications/history/{customerId}"
        ));
        return ResponseEntity.ok(response);
    }
}
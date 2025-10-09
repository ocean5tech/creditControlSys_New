package com.creditcontrol.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RiskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
        System.out.println("🚀 Risk Service started successfully on port 8083");
        System.out.println("📊 Health check: http://localhost:8083/actuator/health");
        System.out.println("🔗 API base: http://localhost:8083/api/v1/risk");
    }
}
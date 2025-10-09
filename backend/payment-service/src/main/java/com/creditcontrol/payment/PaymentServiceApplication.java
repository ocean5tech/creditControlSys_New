package com.creditcontrol.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        System.out.println("🚀 Payment Service started successfully on port 8084");
        System.out.println("📊 Health check: http://localhost:8084/actuator/health");
        System.out.println("🔗 API base: http://localhost:8084/api/v1/payments");
    }
}
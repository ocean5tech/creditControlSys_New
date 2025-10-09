package com.creditcontrol.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("🚀 Notification Service started successfully on port 8086");
        System.out.println("📊 Health check: http://localhost:8086/actuator/health");
        System.out.println("🔗 API base: http://localhost:8086/api/v1/notifications");
    }
}
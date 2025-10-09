package com.creditcontrol.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
        System.out.println("🚀 Report Service started successfully on port 8085");
        System.out.println("📊 Health check: http://localhost:8085/actuator/health");
        System.out.println("🔗 API base: http://localhost:8085/api/v1/reports");
    }
}
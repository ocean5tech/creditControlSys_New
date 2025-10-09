package com.creditcontrol.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Credit Service Application
 * 信用管理服务主应用类
 */
@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
public class CreditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
        System.out.println("🚀 Credit Service started successfully on port 8082");
        System.out.println("📊 Health check: http://localhost:8082/actuator/health");
        System.out.println("🔗 API base: http://localhost:8082/api/v1/credit");
    }
}
package com.creditcontrol.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Customer Service Application
 * 客户管理微服务主启动类
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableCaching
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
        System.out.println("🚀 Customer Service started successfully on port 8080 (mapped to 8081)");
        System.out.println("📊 Health check: http://localhost:8081/actuator/health");
        System.out.println("🔗 API base: http://localhost:8081/api/v1/customers");
    }
}
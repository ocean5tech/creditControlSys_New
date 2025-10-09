package com.creditcontrol.customer.controller;

import com.creditcontrol.customer.dto.*;
import com.creditcontrol.customer.exception.CustomerNotFoundException;
import com.creditcontrol.customer.exception.DuplicateCustomerException;
import com.creditcontrol.customer.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Customer REST API控制器
 * 提供客户管理的RESTful接口
 */
@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "http://localhost:3000")
@Validated
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * 根据ID获取客户信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable Integer id) {
        log.info("API_REQUEST: GET /api/v1/customers/{}", id);
        
        try {
            CustomerDto customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(ApiResponse.success(customer, "Customer retrieved successfully"));
        } catch (CustomerNotFoundException e) {
            log.warn("CUSTOMER_NOT_FOUND: id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
        }
    }

    /**
     * 根据客户代码获取客户信息
     */
    @GetMapping("/code/{customerCode}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomerByCode(@PathVariable String customerCode) {
        log.info("API_REQUEST: GET /api/v1/customers/code/{}", customerCode);
        
        try {
            CustomerDto customer = customerService.getCustomerByCode(customerCode);
            return ResponseEntity.ok(ApiResponse.success(customer, "Customer retrieved successfully"));
        } catch (CustomerNotFoundException e) {
            log.warn("CUSTOMER_NOT_FOUND: customerCode: {}", customerCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
        }
    }

    /**
     * 搜索客户 - 支持分页和模糊查询
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> searchCustomers(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        
        log.info("API_REQUEST: GET /api/v1/customers/search - q: {}, page: {}, size: {}, includeInactive: {}", 
                q, page, size, includeInactive);
        
        PageResponse<CustomerDto> result = customerService.searchCustomers(q, page, size, includeInactive);
        
        String message = String.format("Found %d customers", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * 获取所有活跃客户 (简化版搜索)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> getAllCustomers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        log.info("API_REQUEST: GET /api/v1/customers - page: {}, size: {}", page, size);
        
        PageResponse<CustomerDto> result = customerService.searchCustomers("", page, size, false);
        
        String message = String.format("Retrieved %d active customers", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * 创建新客户
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(
            @RequestBody @Valid CustomerCreateRequest request) {
        
        log.info("API_REQUEST: POST /api/v1/customers - customerCode: {}, companyName: {}", 
                request.getCustomerCode(), request.getCompanyName());
        
        try {
            CustomerDto customer = customerService.createCustomer(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(customer, "Customer created successfully"));
        } catch (DuplicateCustomerException e) {
            log.warn("DUPLICATE_CUSTOMER: customerCode: {}", request.getCustomerCode());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Customer code already exists"));
        }
    }

    /**
     * 更新客户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable Integer id,
            @RequestBody @Valid CustomerCreateRequest request) {
        
        log.info("API_REQUEST: PUT /api/v1/customers/{} - customerCode: {}", id, request.getCustomerCode());
        
        try {
            CustomerDto customer = customerService.updateCustomer(id, request);
            return ResponseEntity.ok(ApiResponse.success(customer, "Customer updated successfully"));
        } catch (CustomerNotFoundException e) {
            log.warn("CUSTOMER_NOT_FOUND: id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
        } catch (DuplicateCustomerException e) {
            log.warn("DUPLICATE_CUSTOMER: customerCode: {}", request.getCustomerCode());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Customer code already exists"));
        }
    }

    /**
     * 停用客户
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(
            @PathVariable Integer id,
            @RequestParam(required = false) String deactivatedBy) {
        
        log.info("API_REQUEST: PATCH /api/v1/customers/{}/deactivate - deactivatedBy: {}", id, deactivatedBy);
        
        try {
            customerService.deactivateCustomer(id, deactivatedBy);
            return ResponseEntity.ok(ApiResponse.success(null, "Customer deactivated successfully"));
        } catch (CustomerNotFoundException e) {
            log.warn("CUSTOMER_NOT_FOUND: id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
        }
    }

    /**
     * 激活客户
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(
            @PathVariable Integer id,
            @RequestParam(required = false) String activatedBy) {
        
        log.info("API_REQUEST: PATCH /api/v1/customers/{}/activate - activatedBy: {}", id, activatedBy);
        
        try {
            customerService.activateCustomer(id, activatedBy);
            return ResponseEntity.ok(ApiResponse.success(null, "Customer activated successfully"));
        } catch (CustomerNotFoundException e) {
            log.warn("CUSTOMER_NOT_FOUND: id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
        }
    }

    /**
     * 获取客户统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CustomerStatsDto>> getCustomerStats() {
        log.info("API_REQUEST: GET /api/v1/customers/stats");
        
        CustomerStatsDto stats = customerService.getCustomerStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Customer statistics retrieved successfully"));
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("API_REQUEST: GET /api/v1/customers/health");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "customer-service");
        response.put("timestamp", Instant.now().toString());
        response.put("version", "1.0.0");
        
        // 检查数据库连接
        try {
            long customerCount = customerService.getCustomerStats().getTotalCustomers();
            response.put("database", "UP");
            response.put("totalCustomers", customerCount);
        } catch (Exception e) {
            log.error("HEALTH_CHECK_DB_ERROR: {}", e.getMessage(), e);
            response.put("database", "DOWN");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * API信息端点
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        log.debug("API_REQUEST: GET /api/v1/customers/info");
        
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Customer Management Service");
        info.put("version", "1.0.0");
        info.put("description", "Microservice for customer data management");
        info.put("baseUrl", "/api/v1/customers");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /", "Get all customers (paginated)");
        endpoints.put("GET /search", "Search customers with query");
        endpoints.put("GET /{id}", "Get customer by ID");
        endpoints.put("GET /code/{code}", "Get customer by code");
        endpoints.put("POST /", "Create new customer");
        endpoints.put("PUT /{id}", "Update customer");
        endpoints.put("PATCH /{id}/activate", "Activate customer");
        endpoints.put("PATCH /{id}/deactivate", "Deactivate customer");
        endpoints.put("GET /stats", "Get customer statistics");
        endpoints.put("GET /health", "Health check");
        
        info.put("endpoints", endpoints);
        info.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(info);
    }
}
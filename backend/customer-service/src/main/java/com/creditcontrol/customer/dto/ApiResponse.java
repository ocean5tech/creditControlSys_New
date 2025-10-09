package com.creditcontrol.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一API响应格式
 * 包装所有API响应，提供一致的数据结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    
    private T data;
    
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
    
    private String requestId;
    
    // 成功响应构造方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), null);
    }
    
    // 错误响应构造方法
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), null);
    }
    
    public static <T> ApiResponse<T> error(String message, String requestId) {
        return new ApiResponse<>(false, null, message, Instant.now(), requestId);
    }
}
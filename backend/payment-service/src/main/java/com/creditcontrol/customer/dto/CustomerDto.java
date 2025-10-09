package com.creditcontrol.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Customer数据传输对象
 * 用于API响应和前端数据交互
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Integer customerId;
    
    private String customerCode;
    
    private String companyName;
    
    private String contactPerson;
    
    private String phone;
    
    private String email;
    
    private String address;
    
    private String industry;
    
    private String registrationNumber;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Timestamp createdDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Timestamp updatedDate;
    
    // 扩展字段 (不存储在数据库中)
    private String displayName;
    
    private boolean active;
    
    // 统计字段 (用于dashboard)
    private Long totalTransactions;
    
    private Double totalAmount;
}
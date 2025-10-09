package com.creditcontrol.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Customer创建请求DTO
 * 用于客户创建API的请求体验证
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateRequest {

    @NotBlank(message = "Customer code is required")
    @Pattern(regexp = "^[A-Z0-9]{4,20}$", 
             message = "Customer code must be 4-20 alphanumeric characters")
    private String customerCode;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String companyName;

    @Size(max = 100, message = "Contact person name must not exceed 100 characters")
    private String contactPerson;

    @Pattern(regexp = "^[+]?[0-9\\-\\s()]{0,50}$", 
             message = "Invalid phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    private String createdBy;
}
package com.creditcontrol.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Customer实体类
 * 映射到现有数据库的customers表
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_code", unique = true, nullable = false, length = 20)
    private String customerCode;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;

    // 业务方法
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (updatedDate == null) {
            updatedDate = new Timestamp(System.currentTimeMillis());
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * 检查客户是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return companyName != null ? companyName : customerCode;
    }
}
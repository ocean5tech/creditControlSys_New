package com.creditcontrol.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Customer统计信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDto {
    
    private long totalCustomers;
    
    private long activeCustomers;
    
    private long inactiveCustomers;
    
    private int totalIndustries;
    
    private List<String> industries;
}
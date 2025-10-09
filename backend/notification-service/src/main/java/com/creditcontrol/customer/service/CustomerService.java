package com.creditcontrol.customer.service;

import com.creditcontrol.customer.dto.CustomerCreateRequest;
import com.creditcontrol.customer.dto.CustomerDto;
import com.creditcontrol.customer.dto.CustomerStatsDto;
import com.creditcontrol.customer.dto.PageResponse;
import com.creditcontrol.customer.entity.Customer;
import com.creditcontrol.customer.exception.CustomerNotFoundException;
import com.creditcontrol.customer.exception.DuplicateCustomerException;
import com.creditcontrol.customer.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer业务服务层
 * 处理客户相关的所有业务逻辑
 */
@Service
@Transactional
@Slf4j
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * 根据ID获取客户信息 (带缓存)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "#customerId")
    public CustomerDto getCustomerById(Integer customerId) {
        log.info("SERVICE_CALL: getCustomerById - customerId: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("CUSTOMER_NOT_FOUND: customerId: {}", customerId);
                    return new CustomerNotFoundException("Customer not found with ID: " + customerId);
                });
        
        CustomerDto customerDto = convertToDto(customer);
        log.info("CUSTOMER_FOUND: customerId: {}, customerCode: {}, companyName: {}", 
                customerId, customer.getCustomerCode(), customer.getCompanyName());
        
        return customerDto;
    }

    /**
     * 根据客户代码获取客户信息
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'code:' + #customerCode")
    public CustomerDto getCustomerByCode(String customerCode) {
        log.info("SERVICE_CALL: getCustomerByCode - customerCode: {}", customerCode);
        
        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> {
                    log.warn("CUSTOMER_NOT_FOUND: customerCode: {}", customerCode);
                    return new CustomerNotFoundException("Customer not found with code: " + customerCode);
                });
        
        return convertToDto(customer);
    }

    /**
     * 搜索客户 (支持分页)
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> searchCustomers(String query, int page, int size, boolean includeInactive) {
        log.info("SERVICE_CALL: searchCustomers - query: {}, page: {}, size: {}, includeInactive: {}", 
                query, page, size, includeInactive);
        
        // 验证分页参数
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 10; // 限制最大页面大小
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("companyName").ascending());
        
        Page<Customer> customerPage;
        if (query == null || query.trim().isEmpty()) {
            // 如果没有搜索条件，返回所有客户
            if (includeInactive) {
                customerPage = customerRepository.findAll(pageable);
            } else {
                customerPage = customerRepository.findRecentActiveCustomers(pageable);
            }
        } else {
            // 根据搜索条件查询
            if (includeInactive) {
                customerPage = customerRepository.searchAllCustomers(query.trim(), pageable);
            } else {
                customerPage = customerRepository.searchActiveCustomers(query.trim(), pageable);
            }
        }
        
        List<CustomerDto> customerDtos = customerPage.getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("SEARCH_RESULT: Found {} customers for query: '{}', page: {}, total: {}", 
                customerDtos.size(), query, page, customerPage.getTotalElements());
        
        return PageResponse.of(customerDtos, customerPage.getTotalElements(), page, size);
    }

    /**
     * 创建新客户
     */
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDto createCustomer(CustomerCreateRequest request) {
        log.info("SERVICE_CALL: createCustomer - customerCode: {}, companyName: {}", 
                request.getCustomerCode(), request.getCompanyName());
        
        // 检查客户代码唯一性
        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            log.warn("DUPLICATE_CUSTOMER_CODE: customerCode: {}", request.getCustomerCode());
            throw new DuplicateCustomerException("Customer code already exists: " + request.getCustomerCode());
        }
        
        // 生成新的客户ID (简单方案，生产环境可能需要更复杂的ID生成策略)
        Integer newCustomerId = generateNewCustomerId();
        
        // 创建客户实体
        Customer customer = new Customer();
        customer.setCustomerId(newCustomerId);
        customer.setCustomerCode(request.getCustomerCode().toUpperCase());
        customer.setCompanyName(request.getCompanyName().trim());
        customer.setContactPerson(request.getContactPerson());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setIndustry(request.getIndustry());
        customer.setRegistrationNumber(request.getRegistrationNumber());
        customer.setStatus("ACTIVE");
        customer.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        customer.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        
        Customer savedCustomer = customerRepository.save(customer);
        
        log.info("CUSTOMER_CREATED: customerId: {}, customerCode: {}, companyName: {}", 
                savedCustomer.getCustomerId(), savedCustomer.getCustomerCode(), savedCustomer.getCompanyName());
        
        return convertToDto(savedCustomer);
    }

    /**
     * 更新客户信息
     */
    @Transactional
    @CacheEvict(value = "customers", key = "#customerId")
    public CustomerDto updateCustomer(Integer customerId, CustomerCreateRequest request) {
        log.info("SERVICE_CALL: updateCustomer - customerId: {}, customerCode: {}", 
                customerId, request.getCustomerCode());
        
        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        
        // 检查客户代码唯一性 (如果代码有变更)
        if (!existingCustomer.getCustomerCode().equals(request.getCustomerCode())) {
            if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
                throw new DuplicateCustomerException("Customer code already exists: " + request.getCustomerCode());
            }
            existingCustomer.setCustomerCode(request.getCustomerCode().toUpperCase());
        }
        
        // 更新字段
        existingCustomer.setCompanyName(request.getCompanyName().trim());
        existingCustomer.setContactPerson(request.getContactPerson());
        existingCustomer.setPhone(request.getPhone());
        existingCustomer.setEmail(request.getEmail());
        existingCustomer.setAddress(request.getAddress());
        existingCustomer.setIndustry(request.getIndustry());
        existingCustomer.setRegistrationNumber(request.getRegistrationNumber());
        existingCustomer.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        
        log.info("CUSTOMER_UPDATED: customerId: {}, customerCode: {}", 
                updatedCustomer.getCustomerId(), updatedCustomer.getCustomerCode());
        
        return convertToDto(updatedCustomer);
    }

    /**
     * 停用客户 (软删除)
     */
    @Transactional
    @CacheEvict(value = "customers", key = "#customerId")
    public void deactivateCustomer(Integer customerId, String deactivatedBy) {
        log.info("SERVICE_CALL: deactivateCustomer - customerId: {}, deactivatedBy: {}", 
                customerId, deactivatedBy);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        
        customer.setStatus("INACTIVE");
        customer.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        
        customerRepository.save(customer);
        
        log.info("CUSTOMER_DEACTIVATED: customerId: {}, customerCode: {}", 
                customer.getCustomerId(), customer.getCustomerCode());
    }

    /**
     * 激活客户
     */
    @Transactional
    @CacheEvict(value = "customers", key = "#customerId")
    public void activateCustomer(Integer customerId, String activatedBy) {
        log.info("SERVICE_CALL: activateCustomer - customerId: {}, activatedBy: {}", 
                customerId, activatedBy);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        
        customer.setStatus("ACTIVE");
        customer.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        
        customerRepository.save(customer);
        
        log.info("CUSTOMER_ACTIVATED: customerId: {}, customerCode: {}", 
                customer.getCustomerId(), customer.getCustomerCode());
    }

    /**
     * 获取客户统计信息
     */
    @Transactional(readOnly = true)
    public CustomerStatsDto getCustomerStats() {
        log.info("SERVICE_CALL: getCustomerStats");
        
        long totalCustomers = customerRepository.count();
        long activeCustomers = customerRepository.countActiveCustomers();
        List<String> industries = customerRepository.findDistinctIndustries();
        
        CustomerStatsDto stats = CustomerStatsDto.builder()
                .totalCustomers(totalCustomers)
                .activeCustomers(activeCustomers)
                .inactiveCustomers(totalCustomers - activeCustomers)
                .totalIndustries(industries.size())
                .industries(industries)
                .build();
        
        log.info("CUSTOMER_STATS: total: {}, active: {}, inactive: {}, industries: {}", 
                totalCustomers, activeCustomers, stats.getInactiveCustomers(), industries.size());
        
        return stats;
    }

    /**
     * 实体转DTO
     */
    private CustomerDto convertToDto(Customer customer) {
        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .customerCode(customer.getCustomerCode())
                .companyName(customer.getCompanyName())
                .contactPerson(customer.getContactPerson())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .industry(customer.getIndustry())
                .registrationNumber(customer.getRegistrationNumber())
                .status(customer.getStatus())
                .createdDate(customer.getCreatedDate())
                .updatedDate(customer.getUpdatedDate())
                .displayName(customer.getDisplayName())
                .active(customer.isActive())
                .build();
    }

    /**
     * 生成新的客户ID
     * 简单实现，生产环境可能需要更复杂的策略
     */
    private Integer generateNewCustomerId() {
        // 获取当前最大ID并加1
        Integer maxId = customerRepository.findAll(PageRequest.of(0, 1, Sort.by("customerId").descending()))
                .stream()
                .map(Customer::getCustomerId)
                .findFirst()
                .orElse(0);
        
        return maxId + 1;
    }
}
package com.creditcontrol.customer.repository;

import com.creditcontrol.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Customer数据访问层
 * 提供客户数据的CRUD操作和查询功能
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /**
     * 根据客户代码查找客户
     */
    Optional<Customer> findByCustomerCode(String customerCode);

    /**
     * 检查客户代码是否存在
     */
    boolean existsByCustomerCode(String customerCode);

    /**
     * 搜索客户 - 支持公司名称和客户代码模糊搜索
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "c.status = 'ACTIVE'")
    Page<Customer> searchActiveCustomers(@Param("query") String query, Pageable pageable);

    /**
     * 搜索所有状态的客户
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Customer> searchAllCustomers(@Param("query") String query, Pageable pageable);

    /**
     * 根据状态查找客户
     */
    List<Customer> findByStatus(String status);

    /**
     * 统计活跃客户数量
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = 'ACTIVE'")
    long countActiveCustomers();

    /**
     * 根据行业查找客户
     */
    List<Customer> findByIndustryAndStatus(String industry, String status);

    /**
     * 查找最近创建的客户
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' " +
           "ORDER BY c.createdDate DESC")
    Page<Customer> findRecentActiveCustomers(Pageable pageable);

    /**
     * 根据联系人查找客户
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.contactPerson) LIKE LOWER(CONCAT('%', :contactPerson, '%')) AND " +
           "c.status = 'ACTIVE'")
    List<Customer> findByContactPersonContaining(@Param("contactPerson") String contactPerson);

    /**
     * 根据邮箱查找客户
     */
    Optional<Customer> findByEmailAndStatus(String email, String status);

    /**
     * 获取所有行业列表
     */
    @Query("SELECT DISTINCT c.industry FROM Customer c WHERE " +
           "c.industry IS NOT NULL AND c.status = 'ACTIVE' " +
           "ORDER BY c.industry")
    List<String> findDistinctIndustries();

    /**
     * 根据客户ID列表批量查询
     */
    @Query("SELECT c FROM Customer c WHERE c.customerId IN :customerIds AND c.status = 'ACTIVE'")
    List<Customer> findByCustomerIdsAndActive(@Param("customerIds") List<Long> customerIds);

    /**
     * 原生SQL查询 - 复杂查询场景
     */
    @Query(value = "SELECT * FROM customers WHERE customer_id = :id", nativeQuery = true)
    Optional<Customer> findByIdNative(@Param("id") Long id);

    /**
     * 统计各状态客户数量
     */
    @Query("SELECT c.status, COUNT(c) FROM Customer c GROUP BY c.status")
    List<Object[]> countCustomersByStatus();

    /**
     * 查找需要更新的客户 (修改日期较旧)
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "c.updatedDate < :cutoffDate AND c.status = 'ACTIVE' " +
           "ORDER BY c.updatedDate ASC")
    Page<Customer> findCustomersNeedingUpdate(@Param("cutoffDate") java.sql.Timestamp cutoffDate, 
                                              Pageable pageable);
}
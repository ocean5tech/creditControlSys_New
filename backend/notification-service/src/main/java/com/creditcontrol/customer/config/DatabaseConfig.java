package com.creditcontrol.customer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据库配置类
 * 配置数据库连接池和JPA
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * 配置数据源和连接池
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        log.info("DB_CONFIG: Initializing HikariCP connection pool");
        log.info("DB_CONFIG: JDBC URL: {}", jdbcUrl);
        log.info("DB_CONFIG: Username: {}", username);
        log.info("DB_CONFIG: Max pool size: {}", maximumPoolSize);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setPoolName("CustomerServiceCP");
        config.setAutoCommit(false);
        
        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // 性能优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        log.info("DB_CONFIG: HikariCP connection pool initialized successfully");
        return dataSource;
    }

    /**
     * 配置EntityManagerFactory
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("JPA_CONFIG: Configuring EntityManagerFactory");
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.creditcontrol.customer.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        
        em.setJpaProperties(properties);
        
        log.info("JPA_CONFIG: EntityManagerFactory configured successfully");
        return em;
    }

    /**
     * 配置事务管理器
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("TX_CONFIG: Configuring JPA transaction manager");
        
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        log.info("TX_CONFIG: Transaction manager configured successfully");
        return transactionManager;
    }
}
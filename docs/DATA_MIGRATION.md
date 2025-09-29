# 数据迁移策略和执行计划

## 数据迁移概述

### 迁移目标
- **零数据丢失**: 确保100%数据完整性迁移
- **业务连续性**: 最小化业务中断时间
- **性能优化**: 利用云原生优势提升数据处理性能  
- **合规保障**: 满足数据安全和隐私保护要求

### 迁移策略
- **渐进式迁移**: 分阶段、分模块平滑迁移
- **双写验证**: 新旧系统并行运行验证数据一致性
- **回滚机制**: 完整的数据回滚和恢复策略

---

## 数据现状分析

### Legacy数据库结构分析

#### 数据量统计 (基于当前测试环境)
| 表名 | 记录数 | 数据大小 | 增长率 | 迁移优先级 |
|------|--------|----------|--------|------------|
| `customers` | 5 | 2KB | 10条/月 | **高** |
| `customer_credit` | 5 | 1.5KB | 5条/月 | **高** |
| `daily_transactions` | 10 | 8KB | 500条/月 | **高** |
| `customer_summaries` | 3 | 2KB | 15条/月 | **中** |
| `payment_history` | 8 | 5KB | 200条/月 | **中** |
| `batch_processing_log` | 12 | 3KB | 30条/月 | **低** |
| `system_config` | 5 | 1KB | 2条/月 | **中** |
| `access_log` | 50+ | 10KB+ | 1000条/月 | **低** |

#### 数据质量评估
```sql
-- 数据完整性检查
数据质量问题:
├─ 客户表: 0% 重复数据
├─ 信用表: 100% 外键完整性
├─ 交易表: 5% 空值字段 (description)
├─ 付款表: 0% 数据异常
└─ 总体评分: 95% (优秀)

数据标准化需求:
├─ 日期格式: 统一ISO 8601格式
├─ 金额字段: 统一精度(15,2)
├─ 状态字段: 枚举值标准化
└─ 编码格式: UTF-8统一编码
```

### 生产环境数据预估

#### 预期数据规模 (基于业务增长)
| 表名 | 1年后预估 | 3年后预估 | 存储需求 | 备注 |
|------|-----------|-----------|----------|------|
| `customers` | 500条 | 2,000条 | 2MB | 稳定增长 |
| `customer_credit` | 500条 | 2,000条 | 1.5MB | 1:1关系 |
| `daily_transactions` | 50,000条 | 200,000条 | 500MB | 核心业务数据 |
| `customer_summaries` | 6,000条 | 24,000条 | 50MB | 月度汇总 |
| `payment_history` | 20,000条 | 80,000条 | 200MB | 付款记录 |
| `batch_processing_log` | 2,000条 | 8,000条 | 20MB | 系统日志 |
| `system_config` | 50条 | 100条 | 0.1MB | 配置数据 |
| `access_log` | 500,000条 | 2,000,000条 | 2GB | 审计日志 |

---

## Azure目标数据架构

### Azure SQL Database设计

#### 数据库配置
```sql
-- Azure SQL Database配置
服务层级: Standard S2 (50 DTU)
存储容量: 250GB (可扩展至500GB)
备份保留: 7天短期 + 10年长期
地理复制: 启用 (备用区域)
加密: 透明数据加密 (TDE)
```

#### 表结构设计 (主要变更)

##### 1. 客户表 (Customers)
```sql
-- Legacy → Azure 字段映射
CREATE TABLE Customers (
    CustomerId UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), -- 改用GUID
    CustomerCode NVARCHAR(20) NOT NULL UNIQUE,
    CompanyName NVARCHAR(200) NOT NULL,
    ContactPerson NVARCHAR(100),
    Phone NVARCHAR(50),
    Email NVARCHAR(100),
    Address NVARCHAR(MAX),
    Industry NVARCHAR(50),
    RegistrationNumber NVARCHAR(50),
    
    -- 新增字段
    TenantId UNIQUEIDENTIFIER NOT NULL, -- 多租户支持
    ExternalCustomerId NVARCHAR(50), -- 外部系统ID
    CustomerSegment NVARCHAR(30), -- 客户分群
    PreferredLanguage CHAR(2) DEFAULT 'EN', -- 语言偏好
    TimeZone NVARCHAR(50) DEFAULT 'UTC', -- 时区设置
    Tags NVARCHAR(MAX), -- JSON格式标签
    
    -- 审计字段
    CreatedDate DATETIME2 DEFAULT GETUTCDATE(),
    CreatedBy NVARCHAR(100),
    ModifiedDate DATETIME2 DEFAULT GETUTCDATE(),
    ModifiedBy NVARCHAR(100),
    Version ROWVERSION, -- 乐观锁控制
    Status NVARCHAR(20) DEFAULT 'ACTIVE',
    
    -- 软删除支持
    IsDeleted BIT DEFAULT 0,
    DeletedDate DATETIME2,
    DeletedBy NVARCHAR(100)
);

-- 索引优化
CREATE INDEX IX_Customers_Code ON Customers(CustomerCode) WHERE IsDeleted = 0;
CREATE INDEX IX_Customers_Tenant ON Customers(TenantId) WHERE IsDeleted = 0;
CREATE INDEX IX_Customers_Segment ON Customers(CustomerSegment) WHERE IsDeleted = 0;
```

##### 2. 客户信用表 (CustomerCredit)
```sql
CREATE TABLE CustomerCredit (
    CreditId UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    CustomerId UNIQUEIDENTIFIER REFERENCES Customers(CustomerId),
    
    -- 基础信用信息
    CreditLimit DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    AvailableCredit DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    CreditRating NVARCHAR(10) DEFAULT 'C',
    RiskScore INTEGER DEFAULT 50,
    
    -- 增强字段
    CreditScoreHistory NVARCHAR(MAX), -- JSON格式历史
    ModelVersion NVARCHAR(20), -- ML模型版本
    ModelPrediction DECIMAL(5,4), -- 预测违约概率
    ModelExplanation NVARCHAR(MAX), -- 模型解释(JSON)
    RiskFactors NVARCHAR(MAX), -- 风险因子(JSON)
    
    -- 审批流程
    LastReviewDate DATE,
    NextReviewDate DATE,
    CreditOfficer NVARCHAR(100),
    ApprovalStatus NVARCHAR(20) DEFAULT 'PENDING',
    ApprovalWorkflowId UNIQUEIDENTIFIER, -- 工作流实例ID
    
    -- 监管合规
    RegulatoryClassification NVARCHAR(50),
    ComplianceFlags NVARCHAR(MAX), -- JSON格式合规标记
    
    -- 审计字段 (同customers表)
    CreatedDate DATETIME2 DEFAULT GETUTCDATE(),
    ModifiedDate DATETIME2 DEFAULT GETUTCDATE(),
    Version ROWVERSION,
    TenantId UNIQUEIDENTIFIER NOT NULL
);
```

##### 3. 交易表 (Transactions)
```sql
CREATE TABLE Transactions (
    TransactionId UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    CustomerId UNIQUEIDENTIFIER REFERENCES Customers(CustomerId),
    
    -- 基础交易信息
    TransactionDate DATE NOT NULL,
    TransactionType NVARCHAR(20) NOT NULL,
    ReferenceNumber NVARCHAR(50) UNIQUE NOT NULL,
    Amount DECIMAL(18,2) NOT NULL,
    Currency CHAR(3) DEFAULT 'USD', -- 多币种支持
    
    -- 状态和处理
    Status NVARCHAR(20) DEFAULT 'PENDING',
    ProcessingDate DATETIME2,
    SettlementDate DATE,
    
    -- 业务信息
    OutstandingAmount DECIMAL(18,2) DEFAULT 0.00,
    DueDate DATE,
    PaymentDate DATE,
    PaymentMethod NVARCHAR(30),
    Description NVARCHAR(MAX),
    
    -- 分类和标记
    Category NVARCHAR(50),
    SubCategory NVARCHAR(50),
    BusinessLine NVARCHAR(50),
    ProductCode NVARCHAR(30),
    
    -- 处理信息
    ProcessedBy NVARCHAR(100),
    ProcessingBatchId UNIQUEIDENTIFIER,
    
    -- 外部系统集成
    ExternalTransactionId NVARCHAR(100),
    SourceSystem NVARCHAR(50),
    
    -- 审计和合规
    TenantId UNIQUEIDENTIFIER NOT NULL,
    CreatedDate DATETIME2 DEFAULT GETUTCDATE(),
    ModifiedDate DATETIME2 DEFAULT GETUTCDATE(),
    Version ROWVERSION
);

-- 性能优化索引
CREATE INDEX IX_Transactions_Customer_Date ON Transactions(CustomerId, TransactionDate);
CREATE INDEX IX_Transactions_Reference ON Transactions(ReferenceNumber);
CREATE INDEX IX_Transactions_Status ON Transactions(Status, TransactionDate);
CREATE INDEX IX_Transactions_Batch ON Transactions(ProcessingBatchId);
```

### Redis缓存数据结构

#### 缓存策略设计
```redis
-- 客户信息缓存 (1小时TTL)
KEY: customer:{customerId}
VALUE: JSON格式客户完整信息
EXPIRE: 3600秒

-- 信用信息缓存 (30分钟TTL)  
KEY: credit:{customerId}
VALUE: JSON格式信用信息
EXPIRE: 1800秒

-- 会话缓存 (30分钟TTL)
KEY: session:{sessionId}
VALUE: 用户会话信息
EXPIRE: 1800秒

-- 查询结果缓存 (10分钟TTL)
KEY: query:{hash}
VALUE: 查询结果JSON
EXPIRE: 600秒

-- 计数器缓存 (实时更新)
KEY: counter:transactions:{date}
VALUE: 日交易量计数
EXPIRE: 86400秒 (24小时)
```

---

## 迁移执行计划

### Phase 1: 环境准备和基础数据 (Week 9)

#### 1.1 Azure环境搭建
```bash
# Azure资源创建脚本
az group create --name creditcontrol-prod --location eastus

# SQL Database创建
az sql server create \
  --name creditcontrol-sql-server \
  --resource-group creditcontrol-prod \
  --admin-user sqladmin \
  --admin-password 'SecurePassword123!'

az sql db create \
  --server creditcontrol-sql-server \
  --resource-group creditcontrol-prod \
  --name creditcontrol-db \
  --service-objective S2
```

#### 1.2 数据库架构创建
```sql
-- 执行顺序
1. 创建表结构
2. 创建索引
3. 创建约束
4. 创建视图
5. 创建存储过程
6. 配置权限
```

#### 1.3 参考数据迁移
| 数据类型 | 迁移内容 | 验证方法 |
|----------|----------|----------|
| **系统配置** | system_config → Configuration | 记录数对比 |
| **代码表** | 枚举值、状态码 | 完整性检查 |
| **用户权限** | 角色和权限配置 | 权限测试 |

### Phase 2: 主数据迁移 (Week 10)

#### 2.1 客户数据迁移
```sql
-- 迁移脚本示例
DECLARE @MigrationBatchSize INT = 1000;
DECLARE @StartId INT = 1;
DECLARE @MaxId INT;

SELECT @MaxId = MAX(customer_id) FROM legacy.customers;

WHILE @StartId <= @MaxId
BEGIN
    INSERT INTO azure.Customers (
        CustomerCode, CompanyName, ContactPerson, 
        Phone, Email, Address, Industry, 
        RegistrationNumber, TenantId, Status,
        CreatedDate, CreatedBy
    )
    SELECT 
        customer_code, company_name, contact_person,
        phone, email, address, industry,
        registration_number, 
        'DEFAULT_TENANT_ID', -- 默认租户
        status,
        created_date,
        'MIGRATION_SYSTEM'
    FROM legacy.customers 
    WHERE customer_id BETWEEN @StartId AND @StartId + @MigrationBatchSize - 1;
    
    SET @StartId = @StartId + @MigrationBatchSize;
    
    -- 记录迁移进度
    INSERT INTO MigrationLog (TableName, BatchStart, BatchEnd, RecordCount, Status)
    VALUES ('customers', @StartId - @MigrationBatchSize, @StartId - 1, @@ROWCOUNT, 'COMPLETED');
END;
```

#### 2.2 数据验证检查
```sql
-- 数据一致性验证
SELECT 
    'customers' as TableName,
    COUNT(*) as LegacyCount,
    (SELECT COUNT(*) FROM azure.Customers WHERE IsDeleted = 0) as AzureCount,
    CASE 
        WHEN COUNT(*) = (SELECT COUNT(*) FROM azure.Customers WHERE IsDeleted = 0) 
        THEN 'PASS' 
        ELSE 'FAIL' 
    END as ValidationResult
FROM legacy.customers;
```

### Phase 3: 交易数据迁移 (Week 11)

#### 3.1 历史交易数据迁移
```python
# Python迁移脚本示例
import pandas as pd
from sqlalchemy import create_engine
import logging

def migrate_transactions_batch(start_date, end_date):
    # Legacy数据库连接
    legacy_engine = create_engine('postgresql://legacy_conn_string')
    
    # Azure数据库连接  
    azure_engine = create_engine('mssql+pyodbc://azure_conn_string')
    
    # 分批读取Legacy数据
    query = """
    SELECT * FROM daily_transactions 
    WHERE transaction_date BETWEEN %s AND %s
    ORDER BY transaction_id
    """
    
    batch_size = 5000
    offset = 0
    
    while True:
        # 读取批次数据
        df = pd.read_sql(query + f" LIMIT {batch_size} OFFSET {offset}", 
                        legacy_engine, 
                        params=[start_date, end_date])
        
        if df.empty:
            break
            
        # 数据转换
        df_transformed = transform_transaction_data(df)
        
        # 写入Azure
        df_transformed.to_sql('Transactions', azure_engine, 
                            if_exists='append', index=False)
        
        logging.info(f"Migrated batch: offset {offset}, count {len(df)}")
        offset += batch_size

def transform_transaction_data(df):
    """数据转换逻辑"""
    # 生成新的GUID主键
    df['TransactionId'] = df.apply(lambda x: str(uuid.uuid4()), axis=1)
    
    # 日期格式转换
    df['TransactionDate'] = pd.to_datetime(df['transaction_date']).dt.date
    
    # 字段映射
    field_mapping = {
        'customer_id': 'CustomerId',
        'transaction_type': 'TransactionType',
        'reference_number': 'ReferenceNumber',
        'amount': 'Amount',
        # ... 其他字段映射
    }
    
    df = df.rename(columns=field_mapping)
    
    # 添加默认值
    df['TenantId'] = 'DEFAULT_TENANT_ID'
    df['Currency'] = 'USD'
    df['Status'] = 'COMPLETED'
    
    return df
```

#### 3.2 增量数据同步
```sql
-- 增量同步触发器 (Legacy端)
CREATE OR REPLACE FUNCTION sync_transaction_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- 记录变更到同步表
    INSERT INTO transaction_sync_log (
        operation, 
        transaction_id, 
        change_data, 
        change_timestamp
    ) VALUES (
        TG_OP,
        COALESCE(NEW.transaction_id, OLD.transaction_id),
        row_to_json(COALESCE(NEW, OLD)),
        CURRENT_TIMESTAMP
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transaction_sync_trigger
    AFTER INSERT OR UPDATE OR DELETE
    ON daily_transactions
    FOR EACH ROW
    EXECUTE FUNCTION sync_transaction_changes();
```

### Phase 4: 验证和切换 (Week 12)

#### 4.1 数据完整性验证
```sql
-- 全面数据验证报告
WITH ValidationSummary AS (
    SELECT 
        'customers' as TableName,
        (SELECT COUNT(*) FROM legacy.customers) as LegacyCount,
        (SELECT COUNT(*) FROM azure.Customers WHERE IsDeleted = 0) as AzureCount
    UNION ALL
    SELECT 
        'transactions',
        (SELECT COUNT(*) FROM legacy.daily_transactions),
        (SELECT COUNT(*) FROM azure.Transactions)
    UNION ALL
    SELECT 
        'payments',
        (SELECT COUNT(*) FROM legacy.payment_history),
        (SELECT COUNT(*) FROM azure.PaymentHistory)
)
SELECT *,
    CASE 
        WHEN LegacyCount = AzureCount THEN '✓ PASS'
        ELSE '✗ FAIL - Count Mismatch'
    END as ValidationStatus
FROM ValidationSummary;

-- 业务数据验证
SELECT 
    'Amount Validation' as CheckType,
    ABS(legacy_sum - azure_sum) as Difference,
    CASE 
        WHEN ABS(legacy_sum - azure_sum) < 0.01 THEN '✓ PASS'
        ELSE '✗ FAIL - Amount Mismatch'
    END as Status
FROM (
    SELECT 
        (SELECT SUM(amount) FROM legacy.daily_transactions) as legacy_sum,
        (SELECT SUM(Amount) FROM azure.Transactions) as azure_sum
) amounts;
```

#### 4.2 性能基准测试
```sql
-- 查询性能对比测试
DECLARE @StartTime DATETIME2 = GETUTCDATE();

-- 测试查询1: 客户搜索
SELECT COUNT(*) FROM azure.Customers 
WHERE CompanyName LIKE '%Manufacturing%';

DECLARE @Query1Time INT = DATEDIFF(ms, @StartTime, GETUTCDATE());

-- 测试查询2: 交易汇总
SET @StartTime = GETUTCDATE();
SELECT CustomerId, SUM(Amount) 
FROM azure.Transactions 
GROUP BY CustomerId;

DECLARE @Query2Time INT = DATEDIFF(ms, @StartTime, GETUTCDATE());

-- 记录性能基准
INSERT INTO PerformanceBenchmark (QueryType, ExecutionTime, TestDate)
VALUES 
    ('CustomerSearch', @Query1Time, GETUTCDATE()),
    ('TransactionSummary', @Query2Time, GETUTCDATE());
```

---

## 数据同步策略

### 双写模式 (Migration Period)

#### 应用层双写实现
```java
@Service
public class CustomerService {
    
    @Autowired
    private LegacyCustomerRepository legacyRepo;
    
    @Autowired 
    private AzureCustomerRepository azureRepo;
    
    @Autowired
    private FeatureToggle featureToggle;
    
    @Transactional
    public Customer createCustomer(Customer customer) {
        Customer result = null;
        
        try {
            // 主写：Legacy系统
            result = legacyRepo.save(customer);
            
            // 副写：Azure系统 (异步)
            if (featureToggle.isEnabled("azure-write")) {
                CompletableFuture.runAsync(() -> {
                    try {
                        azureRepo.save(convertToAzureCustomer(result));
                    } catch (Exception e) {
                        log.error("Azure write failed", e);
                        // 记录同步失败，后续补偿
                        recordSyncFailure("customer", result.getId(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("Legacy write failed", e);
            throw e;
        }
        
        return result;
    }
    
    public Customer getCustomer(String customerId) {
        // 读取策略：优先读取主系统
        if (featureToggle.isEnabled("azure-read")) {
            try {
                return azureRepo.findById(customerId);
            } catch (Exception e) {
                log.warn("Azure read failed, fallback to legacy", e);
                return legacyRepo.findById(customerId);
            }
        } else {
            return legacyRepo.findById(customerId);
        }
    }
}
```

### 数据一致性保障

#### 1. 事务性保障
```sql
-- 分布式事务处理 (Saga模式)
BEGIN TRANSACTION;
    
    -- Step 1: Legacy数据更新
    UPDATE legacy.customers 
    SET company_name = @newName, updated_date = CURRENT_TIMESTAMP
    WHERE customer_id = @customerId;
    
    -- Step 2: 记录分布式事务日志
    INSERT INTO distributed_transaction_log (
        transaction_id, step, status, data, created_date
    ) VALUES (
        @transactionId, 'legacy_update', 'completed', 
        JSON_OBJECT('customer_id', @customerId), CURRENT_TIMESTAMP
    );
    
COMMIT;

-- Step 3: 异步Azure更新 (补偿事务)
-- 如果失败，会触发补偿逻辑回滚Legacy更改
```

#### 2. 数据校验和修复
```python
class DataConsistencyChecker:
    def __init__(self):
        self.legacy_db = LegacyDatabase()
        self.azure_db = AzureDatabase()
        
    def check_customer_consistency(self, customer_id):
        """检查单个客户数据一致性"""
        legacy_customer = self.legacy_db.get_customer(customer_id)
        azure_customer = self.azure_db.get_customer(customer_id)
        
        inconsistencies = []
        
        # 字段级比较
        field_mapping = {
            'company_name': 'CompanyName',
            'contact_person': 'ContactPerson',
            'phone': 'Phone',
            'email': 'Email'
        }
        
        for legacy_field, azure_field in field_mapping.items():
            if legacy_customer[legacy_field] != azure_customer[azure_field]:
                inconsistencies.append({
                    'field': legacy_field,
                    'legacy_value': legacy_customer[legacy_field],
                    'azure_value': azure_customer[azure_field]
                })
        
        return inconsistencies
    
    def repair_inconsistency(self, customer_id, field, source='legacy'):
        """修复数据不一致"""
        if source == 'legacy':
            # 以Legacy为准修复Azure
            legacy_value = self.legacy_db.get_customer_field(customer_id, field)
            self.azure_db.update_customer_field(customer_id, field, legacy_value)
        else:
            # 以Azure为准修复Legacy  
            azure_value = self.azure_db.get_customer_field(customer_id, field)
            self.legacy_db.update_customer_field(customer_id, field, azure_value)
            
        # 记录修复日志
        self.log_repair_action(customer_id, field, source)

    def bulk_consistency_check(self):
        """批量一致性检查"""
        inconsistent_records = []
        
        # 获取所有客户ID
        customer_ids = self.legacy_db.get_all_customer_ids()
        
        for customer_id in customer_ids:
            inconsistencies = self.check_customer_consistency(customer_id)
            if inconsistencies:
                inconsistent_records.append({
                    'customer_id': customer_id,
                    'issues': inconsistencies
                })
        
        return inconsistent_records
```

---

## 数据备份和恢复

### 备份策略

#### 1. 迁移前完整备份
```bash
#!/bin/bash
# Legacy系统完整备份脚本

BACKUP_DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backup/migration_${BACKUP_DATE}"

# 创建备份目录
mkdir -p $BACKUP_DIR

# PostgreSQL数据库备份
pg_dump -h 35.77.54.203 -U creditapp -d creditcontrol \
    --clean --create --verbose \
    --file="${BACKUP_DIR}/creditcontrol_full.sql"

# 压缩备份文件
gzip "${BACKUP_DIR}/creditcontrol_full.sql"

# 验证备份完整性
if [ $? -eq 0 ]; then
    echo "Backup completed successfully: ${BACKUP_DIR}"
    
    # 备份文件大小和checksum
    ls -lh "${BACKUP_DIR}/creditcontrol_full.sql.gz"
    md5sum "${BACKUP_DIR}/creditcontrol_full.sql.gz" > "${BACKUP_DIR}/checksum.md5"
else
    echo "Backup failed!"
    exit 1
fi
```

#### 2. Azure数据库备份配置
```sql
-- Azure SQL Database自动备份配置
-- 短期备份保留策略
ALTER DATABASE creditcontrol_db 
SET BACKUP_RETENTION = 14 DAYS;

-- 长期备份保留策略  
ALTER DATABASE creditcontrol_db
ADD LONG_TERM_RETENTION (
    WEEKLY_RETENTION = 'P12W',    -- 12周
    MONTHLY_RETENTION = 'P12M',   -- 12个月  
    YEARLY_RETENTION = 'P10Y',    -- 10年
    WEEK_OF_YEAR = 1              -- 每年第1周
);
```

### 恢复策略

#### 1. 快速回滚方案
```sql
-- 迁移失败快速回滚
-- Step 1: 停止应用写入
UPDATE system_config SET config_value = 'MAINTENANCE' 
WHERE config_key = 'SYSTEM_STATUS';

-- Step 2: 切换回Legacy系统
UPDATE system_config SET config_value = 'LEGACY' 
WHERE config_key = 'PRIMARY_DATABASE';

-- Step 3: 数据一致性检查
SELECT COUNT(*) FROM legacy.customers;
SELECT COUNT(*) FROM legacy.daily_transactions;

-- Step 4: 恢复应用服务
UPDATE system_config SET config_value = 'ACTIVE' 
WHERE config_key = 'SYSTEM_STATUS';
```

#### 2. 部分数据恢复
```sql
-- 恢复特定时间点数据
RESTORE DATABASE creditcontrol_db 
FROM AUTOMATED_BACKUP 
WITH POINT_IN_TIME = '2025-01-15 10:30:00',
     REPLACE_EXISTING = TRUE;

-- 恢复特定表数据
RESTORE TABLE azure.Customers 
FROM BACKUP 'backup_customers_20250115'
WHERE ModifiedDate > '2025-01-15 08:00:00';
```

---

## 监控和验证

### 迁移监控面板

#### 实时迁移指标
```sql
-- 迁移进度监控视图
CREATE VIEW MigrationProgress AS
SELECT 
    TableName,
    SUM(RecordCount) as TotalMigrated,
    MAX(BatchEnd) as LastMigratedId,
    MIN(StartTime) as MigrationStartTime,
    MAX(EndTime) as LastBatchTime,
    AVG(DATEDIFF(second, StartTime, EndTime)) as AvgBatchDuration,
    COUNT(*) as TotalBatches,
    SUM(CASE WHEN Status = 'FAILED' THEN 1 ELSE 0 END) as FailedBatches
FROM MigrationLog
GROUP BY TableName;

-- 数据一致性监控
CREATE VIEW DataConsistencyStatus AS
SELECT 
    TableName,
    LegacyCount,
    AzureCount,
    LegacyCount - AzureCount as CountDifference,
    CAST(AzureCount AS FLOAT) / LegacyCount * 100 as CompletionPercentage,
    LastCheckTime
FROM DataValidationLog
WHERE IsLatest = 1;
```

#### 告警配置
```json
{
  "migrationAlerts": {
    "batchFailure": {
      "condition": "FailedBatches > 3",
      "severity": "High",
      "notification": ["email", "sms"],
      "recipients": ["admin@company.com"]
    },
    "dataInconsistency": {
      "condition": "CountDifference > 100",
      "severity": "Critical", 
      "notification": ["email", "slack"],
      "escalation": true
    },
    "performanceDegradation": {
      "condition": "AvgBatchDuration > 300",
      "severity": "Medium",
      "notification": ["email"],
      "autoAction": "reduceLoadLoad"
    }
  }
}
```

### 数据质量监控

#### 自动化验证脚本
```python
class MigrationValidator:
    def __init__(self):
        self.validation_rules = self.load_validation_rules()
        
    def validate_data_integrity(self):
        """数据完整性验证"""
        results = {}
        
        # 记录数量验证
        for table in ['customers', 'transactions', 'payments']:
            legacy_count = self.get_legacy_count(table)
            azure_count = self.get_azure_count(table) 
            
            results[table] = {
                'count_match': legacy_count == azure_count,
                'legacy_count': legacy_count,
                'azure_count': azure_count,
                'difference': abs(legacy_count - azure_count)
            }
            
        # 业务规则验证
        results['business_rules'] = self.validate_business_rules()
        
        # 外键完整性验证
        results['referential_integrity'] = self.validate_foreign_keys()
        
        return results
    
    def validate_business_rules(self):
        """业务规则验证"""
        rules_results = {}
        
        # 规则1: 客户信用额度 >= 已使用额度
        sql = """
        SELECT COUNT(*) FROM azure.CustomerCredit 
        WHERE CreditLimit < (CreditLimit - AvailableCredit)
        """
        invalid_count = self.azure_db.execute_scalar(sql)
        rules_results['credit_limit_rule'] = invalid_count == 0
        
        # 规则2: 交易金额不能为0
        sql = "SELECT COUNT(*) FROM azure.Transactions WHERE Amount = 0"
        zero_amount_count = self.azure_db.execute_scalar(sql)
        rules_results['non_zero_amount'] = zero_amount_count == 0
        
        return rules_results
    
    def generate_validation_report(self):
        """生成验证报告"""
        validation_results = self.validate_data_integrity()
        
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'overall_status': 'PASS' if self.all_validations_passed(validation_results) else 'FAIL',
            'details': validation_results,
            'recommendations': self.generate_recommendations(validation_results)
        }
        
        return report
```

---

## 性能优化

### 迁移性能优化

#### 1. 批处理优化
```python
class OptimizedMigration:
    def __init__(self):
        self.batch_size = 5000  # 动态调整
        self.parallel_workers = 4
        self.connection_pool_size = 10
        
    def adaptive_batch_sizing(self, table_name, current_performance):
        """自适应批次大小调整"""
        target_batch_time = 30  # 目标30秒每批次
        
        if current_performance['avg_batch_time'] > target_batch_time:
            # 处理时间过长，减小批次
            self.batch_size = max(1000, self.batch_size * 0.8)
        elif current_performance['avg_batch_time'] < 10:
            # 处理时间过短，增大批次
            self.batch_size = min(10000, self.batch_size * 1.2)
            
        return self.batch_size
    
    def parallel_migration(self, table_name):
        """并行迁移处理"""
        from concurrent.futures import ThreadPoolExecutor
        
        # 获取数据范围
        total_records = self.get_total_records(table_name)
        chunk_size = total_records // self.parallel_workers
        
        def migrate_chunk(start_id, end_id):
            return self.migrate_data_range(table_name, start_id, end_id)
        
        # 并行执行
        with ThreadPoolExecutor(max_workers=self.parallel_workers) as executor:
            futures = []
            for i in range(self.parallel_workers):
                start_id = i * chunk_size + 1
                end_id = (i + 1) * chunk_size if i < self.parallel_workers - 1 else total_records
                future = executor.submit(migrate_chunk, start_id, end_id)
                futures.append(future)
            
            # 等待所有任务完成
            for future in futures:
                result = future.result()
                self.log_migration_result(result)
```

#### 2. 网络优化
```python
def optimize_network_transfer():
    """网络传输优化"""
    
    # 数据压缩
    compression_config = {
        'enable_gzip': True,
        'compression_level': 6,  # 平衡压缩比和CPU使用
        'batch_compression': True
    }
    
    # 连接池配置
    connection_config = {
        'pool_size': 20,
        'max_overflow': 30,
        'pool_timeout': 30,
        'pool_recycle': 3600,
        'pool_pre_ping': True
    }
    
    # 批量操作优化
    bulk_config = {
        'use_bulk_insert': True,
        'batch_size': 10000,
        'parallel_inserts': True,
        'disable_constraints': True,  # 迁移时暂时禁用
        'disable_triggers': True
    }
    
    return {
        'compression': compression_config,
        'connection': connection_config,
        'bulk_operations': bulk_config
    }
```

### Azure性能调优

#### 1. 数据库性能配置
```sql
-- Azure SQL Database性能调优
-- 自动调优开启
ALTER DATABASE creditcontrol_db 
SET AUTOMATIC_TUNING = AUTO;

-- 查询存储开启
ALTER DATABASE creditcontrol_db 
SET QUERY_STORE = ON 
(
    OPERATION_MODE = READ_WRITE,
    CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30),
    DATA_FLUSH_INTERVAL_SECONDS = 900,
    MAX_STORAGE_SIZE_MB = 1000,
    INTERVAL_LENGTH_MINUTES = 60
);

-- 统计信息自动更新
ALTER DATABASE creditcontrol_db 
SET AUTO_UPDATE_STATISTICS = ON;
ALTER DATABASE creditcontrol_db 
SET AUTO_UPDATE_STATISTICS_ASYNC = ON;
```

#### 2. 索引优化策略
```sql
-- 基于查询模式的索引创建
-- 客户搜索索引
CREATE INDEX IX_Customers_Search 
ON Customers (CompanyName, CustomerCode)
INCLUDE (ContactPerson, Phone, Email)
WHERE IsDeleted = 0;

-- 交易查询索引
CREATE INDEX IX_Transactions_CustomerDate
ON Transactions (CustomerId, TransactionDate)
INCLUDE (Amount, Status, TransactionType);

-- 复合索引优化
CREATE INDEX IX_Credit_RiskScore
ON CustomerCredit (RiskScore, CreditRating)
INCLUDE (CreditLimit, AvailableCredit)
WHERE ApprovalStatus = 'APPROVED';
```

---

**文档版本**: 1.0  
**最后更新**: 2025-01-15  
**数据架构师**: 数据团队
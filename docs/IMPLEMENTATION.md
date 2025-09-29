# 分阶段实施方法论

## 实施概览

### 实施原则
- **风险最小化**: 渐进式迁移，确保业务连续性
- **价值优先**: 优先迁移高价值、低风险功能模块
- **快速反馈**: 短迭代周期，快速验证和调整
- **质量保证**: 每个阶段都有完整的测试和验证

### 实施方法论
采用**敏捷+DevOps+云原生**的实施方法论，结合微服务架构的特点，确保迁移过程的可控性和可靠性。

---

## Phase 1: 基础设施建设 (Week 1-4)

### Week 1: Azure环境搭建

#### 1.1 Azure订阅和资源组设置
```bash
# Azure CLI环境准备脚本
#!/bin/bash

# 登录Azure
az login

# 创建资源组
az group create \
  --name rg-creditcontrol-prod \
  --location "East US" \
  --tags Environment=Production Project=CreditControl

# 创建网络资源
az network vnet create \
  --resource-group rg-creditcontrol-prod \
  --name vnet-creditcontrol \
  --address-prefix 10.0.0.0/16 \
  --subnet-name subnet-app \
  --subnet-prefix 10.0.1.0/24

az network nsg create \
  --resource-group rg-creditcontrol-prod \
  --name nsg-creditcontrol-app
```

#### 1.2 核心服务部署
```yaml
# Azure资源模板 (ARM Template)
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "environmentName": {
      "type": "string",
      "defaultValue": "prod"
    }
  },
  "resources": [
    {
      "type": "Microsoft.Web/serverfarms",
      "apiVersion": "2020-12-01",
      "name": "[concat('plan-creditcontrol-', parameters('environmentName'))]",
      "location": "[resourceGroup().location]",
      "sku": {
        "name": "EP1",
        "tier": "ElasticPremium"
      },
      "properties": {
        "maximumElasticWorkerCount": 20
      }
    },
    {
      "type": "Microsoft.Sql/servers",
      "apiVersion": "2020-11-01-preview",
      "name": "[concat('sql-creditcontrol-', parameters('environmentName'))]",
      "location": "[resourceGroup().location]",
      "properties": {
        "administratorLogin": "sqladmin",
        "administratorLoginPassword": "[parameters('sqlAdminPassword')]"
      },
      "resources": [
        {
          "type": "databases",
          "apiVersion": "2020-11-01-preview",
          "name": "creditcontrol-db",
          "dependsOn": [
            "[resourceId('Microsoft.Sql/servers', concat('sql-creditcontrol-', parameters('environmentName')))]"
          ],
          "properties": {
            "serviceLevelObjective": "S2"
          }
        }
      ]
    }
  ]
}
```

#### 1.3 安全和合规配置
```powershell
# Azure安全配置脚本
# Key Vault创建
$keyVaultName = "kv-creditcontrol-prod"
az keyvault create \
  --name $keyVaultName \
  --resource-group rg-creditcontrol-prod \
  --location "East US" \
  --enabled-for-template-deployment true

# 存储连接字符串和敏感配置
az keyvault secret set \
  --vault-name $keyVaultName \
  --name "SqlConnectionString" \
  --value "Server=tcp:sql-creditcontrol-prod.database.windows.net,1433;Database=creditcontrol-db;..."

# Azure AD B2C租户配置
az ad sp create-for-rbac \
  --name "creditcontrol-app" \
  --role contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/rg-creditcontrol-prod
```

### Week 2: 开发环境配置

#### 2.1 开发工具链搭建
```yaml
# Azure DevOps Pipeline配置
trigger:
  branches:
    include:
    - main
    - develop
    - feature/*

variables:
  buildConfiguration: 'Release'
  azureSubscription: 'Azure-Prod-Connection'

stages:
- stage: Build
  displayName: 'Build and Test'
  jobs:
  - job: Build
    displayName: 'Build Application'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: NodeTool@0
      inputs:
        versionSpec: '18.x'
      displayName: 'Install Node.js'
    
    - script: |
        npm ci
        npm run build
        npm run test:coverage
      displayName: 'npm install, build and test'
    
    - task: PublishCodeCoverageResults@1
      inputs:
        codeCoverageTool: 'Cobertura'
        summaryFileLocation: '$(System.DefaultWorkingDirectory)/coverage/cobertura-coverage.xml'

- stage: Deploy
  displayName: 'Deploy to Azure'
  dependsOn: Build
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  
  jobs:
  - deployment: DeployWeb
    displayName: 'Deploy to Azure Static Web Apps'
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: AzureStaticWebApp@0
            inputs:
              app_location: '/dist'
              api_location: '/api'
              azure_static_web_apps_api_token: $(deployment_token)
```

#### 2.2 本地开发环境
```json
// package.json - 开发依赖
{
  "name": "creditcontrol-frontend",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "test": "vitest",
    "test:coverage": "vitest --coverage",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "format": "prettier --write \"src/**/*.{ts,tsx,json,css,md}\""
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.8.0",
    "@azure/msal-browser": "^2.38.0",
    "@azure/msal-react": "^1.5.0",
    "axios": "^1.3.0",
    "recharts": "^2.5.0",
    "antd": "^5.2.0"
  },
  "devDependencies": {
    "@types/react": "^18.0.27",
    "@types/react-dom": "^18.0.10",
    "@vitejs/plugin-react": "^3.1.0",
    "vite": "^4.1.0",
    "vitest": "^0.28.5",
    "typescript": "^4.9.4",
    "eslint": "^8.35.0",
    "prettier": "^2.8.4"
  }
}
```

### Week 3: CI/CD流水线建立

#### 3.1 持续集成配置
```yaml
# GitHub Actions工作流
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  AZURE_WEBAPP_NAME: creditcontrol-app
  AZURE_WEBAPP_PACKAGE_PATH: '.'
  NODE_VERSION: '18.x'

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: ${{ env.NODE_VERSION }}
        cache: 'npm'
    
    - name: Install dependencies
      run: npm ci
    
    - name: Run linting
      run: npm run lint
    
    - name: Run tests
      run: npm run test:coverage
    
    - name: Build application
      run: npm run build
    
    - name: Upload build artifacts
      uses: actions/upload-artifact@v3
      with:
        name: webapp-build
        path: dist/

  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Run security audit
      run: npm audit --audit-level moderate
    
    - name: Run dependency check
      uses: dependency-check/Dependency-Check_Action@main
      with:
        project: 'creditcontrol'
        path: '.'
        format: 'HTML'

  deploy-staging:
    needs: [build-and-test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    
    steps:
    - name: Download build artifacts
      uses: actions/download-artifact@v3
      with:
        name: webapp-build
        path: dist/
    
    - name: Deploy to Azure Static Web Apps (Staging)
      uses: Azure/static-web-apps-deploy@v1
      with:
        azure_static_web_apps_api_token: ${{ secrets.AZURE_STATIC_WEB_APPS_API_TOKEN_STAGING }}
        repo_token: ${{ secrets.GITHUB_TOKEN }}
        action: "upload"
        app_location: "dist"

  deploy-production:
    needs: [build-and-test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
    - name: Download build artifacts
      uses: actions/download-artifact@v3
      with:
        name: webapp-build
        path: dist/
    
    - name: Deploy to Azure Static Web Apps (Production)
      uses: Azure/static-web-apps-deploy@v1
      with:
        azure_static_web_apps_api_token: ${{ secrets.AZURE_STATIC_WEB_APPS_API_TOKEN_PROD }}
        repo_token: ${{ secrets.GITHUB_TOKEN }}
        action: "upload"
        app_location: "dist"
```

#### 3.2 基础设施即代码(IaC)
```hcl
# Terraform配置文件
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~>3.0"
    }
  }
  
  backend "azurerm" {
    resource_group_name  = "rg-terraform-state"
    storage_account_name = "stterraformstate"
    container_name       = "tfstate"
    key                  = "creditcontrol.terraform.tfstate"
  }
}

provider "azurerm" {
  features {}
}

# 资源组
resource "azurerm_resource_group" "main" {
  name     = "rg-creditcontrol-${var.environment}"
  location = var.location
  
  tags = {
    Environment = var.environment
    Project     = "CreditControl"
    ManagedBy   = "Terraform"
  }
}

# App Service Plan
resource "azurerm_service_plan" "main" {
  name                = "plan-creditcontrol-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  os_type             = "Linux"
  sku_name            = "EP1"
}

# Azure Functions
resource "azurerm_linux_function_app" "main" {
  name                = "func-creditcontrol-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  
  service_plan_id = azurerm_service_plan.main.id
  
  site_config {
    application_stack {
      node_version = "18"
    }
  }
  
  app_settings = {
    "FUNCTIONS_WORKER_RUNTIME" = "node"
    "WEBSITE_NODE_DEFAULT_VERSION" = "~18"
    "SQL_CONNECTION_STRING" = "@Microsoft.KeyVault(VaultName=${azurerm_key_vault.main.name};SecretName=SqlConnectionString)"
  }
}

# SQL Database
resource "azurerm_mssql_server" "main" {
  name                         = "sql-creditcontrol-${var.environment}"
  resource_group_name          = azurerm_resource_group.main.name
  location                     = azurerm_resource_group.main.location
  version                      = "12.0"
  administrator_login          = "sqladmin"
  administrator_login_password = var.sql_admin_password
}

resource "azurerm_mssql_database" "main" {
  name           = "creditcontrol-db"
  server_id      = azurerm_mssql_server.main.id
  sku_name       = "S2"
  
  short_term_retention_policy {
    retention_days = 7
  }
  
  long_term_retention_policy {
    weekly_retention  = "P12W"
    monthly_retention = "P12M"
    yearly_retention  = "P10Y"
    week_of_year      = 1
  }
}
```

### Week 4: 监控和日志系统

#### 4.1 Application Insights配置
```typescript
// Application Insights初始化
import { ApplicationInsights } from '@microsoft/applicationinsights-web';
import { ReactPlugin } from '@microsoft/applicationinsights-react-js';

const reactPlugin = new ReactPlugin();

const appInsights = new ApplicationInsights({
  config: {
    instrumentationKey: process.env.REACT_APP_APPINSIGHTS_KEY,
    extensions: [reactPlugin],
    extensionConfig: {
      [reactPlugin.identifier]: { history: history }
    },
    enableAutoRouteTracking: true,
    enableCorsCorrelation: true,
    enableRequestHeaderTracking: true,
    enableResponseHeaderTracking: true,
  }
});

appInsights.loadAppInsights();
appInsights.trackPageView();

// 自定义遥测
export const trackEvent = (name: string, properties?: any) => {
  appInsights.trackEvent({ name, properties });
};

export const trackException = (error: Error, properties?: any) => {
  appInsights.trackException({ error, properties });
};

export const trackMetric = (name: string, average: number, properties?: any) => {
  appInsights.trackMetric({ name, average }, properties);
};
```

#### 4.2 日志和监控配置
```yaml
# Azure Monitor配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: monitoring-config
data:
  application-insights.json: |
    {
      "instrumentationKey": "${APPINSIGHTS_INSTRUMENTATIONKEY}",
      "sampling": {
        "enabled": true,
        "percentage": 10.0
      },
      "telemetry": {
        "enableDependencyTracking": true,
        "enablePerformanceCounters": true,
        "enableHeartbeat": true
      },
      "logging": {
        "logLevel": {
          "default": "Information",
          "Microsoft": "Warning",
          "Microsoft.Hosting.Lifetime": "Information"
        }
      }
    }
```

---

## Phase 2: 应用迁移开发 (Week 5-8)

### Week 5: React前端应用开发

#### 5.1 项目架构搭建
```typescript
// 项目结构
src/
├── components/          # 可复用组件
│   ├── common/         # 通用组件
│   ├── forms/          # 表单组件
│   └── charts/         # 图表组件
├── pages/              # 页面组件
│   ├── Dashboard/      # 仪表板
│   ├── Customers/      # 客户管理
│   ├── Credit/         # 信用管理
│   ├── Risk/           # 风险评估
│   └── Reports/        # 报表分析
├── services/           # API服务
├── hooks/              # 自定义Hooks
├── utils/              # 工具函数
├── types/              # TypeScript类型定义
├── contexts/           # React Context
└── assets/             # 静态资源

// 主要组件示例
// src/components/CustomerSearch/CustomerSearch.tsx
import React, { useState, useCallback, useMemo } from 'react';
import { Input, Table, Button, Space, Tag } from 'antd';
import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { useCustomerSearch } from '../../hooks/useCustomerSearch';
import { Customer } from '../../types/customer';

interface CustomerSearchProps {
  onCustomerSelect?: (customer: Customer) => void;
}

export const CustomerSearch: React.FC<CustomerSearchProps> = ({ 
  onCustomerSelect 
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const { 
    customers, 
    loading, 
    search, 
    error 
  } = useCustomerSearch();

  const handleSearch = useCallback((value: string) => {
    setSearchTerm(value);
    search(value);
  }, [search]);

  const columns = useMemo(() => [
    {
      title: '客户代码',
      dataIndex: 'customerCode',
      key: 'customerCode',
      render: (code: string) => (
        <Space>
          <UserOutlined />
          <span>{code}</span>
        </Space>
      ),
    },
    {
      title: '公司名称',
      dataIndex: 'companyName',
      key: 'companyName',
    },
    {
      title: '联系人',
      dataIndex: 'contactPerson',
      key: 'contactPerson',
    },
    {
      title: '信用评级',
      dataIndex: 'creditRating',
      key: 'creditRating',
      render: (rating: string) => {
        const color = rating === 'AAA' ? 'green' : 
                     rating === 'AA' ? 'blue' : 
                     rating === 'A' ? 'cyan' : 'orange';
        return <Tag color={color}>{rating}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, customer: Customer) => (
        <Button 
          type="primary" 
          size="small"
          onClick={() => onCustomerSelect?.(customer)}
        >
          选择
        </Button>
      ),
    },
  ], [onCustomerSelect]);

  return (
    <div className="customer-search">
      <div className="search-bar" style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索客户代码或公司名称"
          allowClear
          enterButton={<SearchOutlined />}
          size="large"
          onSearch={handleSearch}
          loading={loading}
        />
      </div>
      
      <Table
        columns={columns}
        dataSource={customers}
        loading={loading}
        rowKey="customerId"
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total, range) => 
            `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
        }}
      />
      
      {error && (
        <div className="error-message" style={{ color: 'red', marginTop: 8 }}>
          搜索失败: {error.message}
        </div>
      )}
    </div>
  );
};
```

#### 5.2 状态管理和数据流
```typescript
// src/contexts/AppContext.tsx
import React, { createContext, useContext, useReducer, useCallback } from 'react';

interface AppState {
  user: User | null;
  currentCustomer: Customer | null;
  theme: 'light' | 'dark';
  notifications: Notification[];
}

type AppAction = 
  | { type: 'SET_USER'; payload: User }
  | { type: 'SET_CURRENT_CUSTOMER'; payload: Customer }
  | { type: 'TOGGLE_THEME' }
  | { type: 'ADD_NOTIFICATION'; payload: Notification }
  | { type: 'REMOVE_NOTIFICATION'; payload: string };

const initialState: AppState = {
  user: null,
  currentCustomer: null,
  theme: 'light',
  notifications: [],
};

const appReducer = (state: AppState, action: AppAction): AppState => {
  switch (action.type) {
    case 'SET_USER':
      return { ...state, user: action.payload };
    case 'SET_CURRENT_CUSTOMER':
      return { ...state, currentCustomer: action.payload };
    case 'TOGGLE_THEME':
      return { ...state, theme: state.theme === 'light' ? 'dark' : 'light' };
    case 'ADD_NOTIFICATION':
      return { 
        ...state, 
        notifications: [...state.notifications, action.payload] 
      };
    case 'REMOVE_NOTIFICATION':
      return {
        ...state,
        notifications: state.notifications.filter(n => n.id !== action.payload)
      };
    default:
      return state;
  }
};

const AppContext = createContext<{
  state: AppState;
  setUser: (user: User) => void;
  setCurrentCustomer: (customer: Customer) => void;
  toggleTheme: () => void;
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
} | null>(null);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ 
  children 
}) => {
  const [state, dispatch] = useReducer(appReducer, initialState);

  const setUser = useCallback((user: User) => {
    dispatch({ type: 'SET_USER', payload: user });
  }, []);

  const setCurrentCustomer = useCallback((customer: Customer) => {
    dispatch({ type: 'SET_CURRENT_CUSTOMER', payload: customer });
  }, []);

  const toggleTheme = useCallback(() => {
    dispatch({ type: 'TOGGLE_THEME' });
  }, []);

  const addNotification = useCallback((notification: Omit<Notification, 'id'>) => {
    const id = Math.random().toString(36).substr(2, 9);
    dispatch({ 
      type: 'ADD_NOTIFICATION', 
      payload: { ...notification, id } 
    });
  }, []);

  const removeNotification = useCallback((id: string) => {
    dispatch({ type: 'REMOVE_NOTIFICATION', payload: id });
  }, []);

  return (
    <AppContext.Provider value={{
      state,
      setUser,
      setCurrentCustomer,
      toggleTheme,
      addNotification,
      removeNotification,
    }}>
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
```

### Week 6: Azure Functions微服务开发

#### 6.1 微服务架构设计
```typescript
// 客户服务 Azure Function
import { AzureFunction, Context, HttpRequest } from "@azure/functions";
import { CustomerService } from '../services/CustomerService';
import { validateRequest } from '../middleware/validation';
import { authenticateUser } from '../middleware/auth';

const httpTrigger: AzureFunction = async (context: Context, req: HttpRequest): Promise<void> => {
    try {
        // 身份验证
        const user = await authenticateUser(req);
        if (!user) {
            context.res = {
                status: 401,
                body: { error: 'Unauthorized' }
            };
            return;
        }

        // 请求验证
        const validationResult = validateRequest(req, 'customerSearch');
        if (!validationResult.isValid) {
            context.res = {
                status: 400,
                body: { error: 'Invalid request', details: validationResult.errors }
            };
            return;
        }

        const customerService = new CustomerService();
        
        switch (req.method) {
            case 'GET':
                if (req.params.id) {
                    // 获取单个客户
                    const customer = await customerService.getById(req.params.id);
                    context.res = {
                        status: 200,
                        body: { success: true, data: customer }
                    };
                } else {
                    // 搜索客户
                    const { query, page, pageSize } = req.query;
                    const result = await customerService.search(query, page, pageSize);
                    context.res = {
                        status: 200,
                        body: { success: true, data: result.customers, total: result.total }
                    };
                }
                break;

            case 'POST':
                // 创建客户
                const newCustomer = await customerService.create(req.body);
                context.res = {
                    status: 201,
                    body: { success: true, data: newCustomer }
                };
                break;

            case 'PUT':
                // 更新客户
                const updatedCustomer = await customerService.update(req.params.id, req.body);
                context.res = {
                    status: 200,
                    body: { success: true, data: updatedCustomer }
                };
                break;

            default:
                context.res = {
                    status: 405,
                    body: { error: 'Method not allowed' }
                };
        }

    } catch (error) {
        context.log.error('Customer API Error:', error);
        
        context.res = {
            status: 500,
            body: { 
                error: 'Internal server error',
                requestId: context.executionContext.invocationId
            }
        };
    }
};

export default httpTrigger;
```

#### 6.2 数据访问层实现
```typescript
// src/services/CustomerService.ts
import { DatabaseService } from './DatabaseService';
import { CacheService } from './CacheService';
import { AuditService } from './AuditService';
import { Customer, CustomerCreateRequest, CustomerUpdateRequest } from '../types/customer';

export class CustomerService {
    private db: DatabaseService;
    private cache: CacheService;
    private audit: AuditService;

    constructor() {
        this.db = new DatabaseService();
        this.cache = new CacheService();
        this.audit = new AuditService();
    }

    async getById(customerId: string): Promise<Customer | null> {
        // 优先从缓存获取
        const cacheKey = `customer:${customerId}`;
        let customer = await this.cache.get<Customer>(cacheKey);
        
        if (!customer) {
            // 从数据库获取
            customer = await this.db.findCustomerById(customerId);
            if (customer) {
                // 缓存结果
                await this.cache.set(cacheKey, customer, 3600); // 1小时缓存
            }
        }

        return customer;
    }

    async search(query: string, page: number = 1, pageSize: number = 10): Promise<{
        customers: Customer[];
        total: number;
    }> {
        const offset = (page - 1) * pageSize;
        
        // 生成缓存键
        const cacheKey = `customer:search:${query}:${page}:${pageSize}`;
        let result = await this.cache.get<{ customers: Customer[]; total: number }>(cacheKey);
        
        if (!result) {
            // 从数据库搜索
            const [customers, total] = await Promise.all([
                this.db.searchCustomers(query, offset, pageSize),
                this.db.countCustomers(query)
            ]);
            
            result = { customers, total };
            
            // 缓存搜索结果（短时间）
            await this.cache.set(cacheKey, result, 600); // 10分钟缓存
        }

        return result;
    }

    async create(customerData: CustomerCreateRequest): Promise<Customer> {
        // 数据验证
        await this.validateCustomerData(customerData);
        
        // 创建客户
        const customer = await this.db.createCustomer(customerData);
        
        // 清除相关缓存
        await this.cache.deletePattern('customer:search:*');
        
        // 记录审计日志
        await this.audit.logCustomerAction('CREATE', customer.customerId, customerData);
        
        return customer;
    }

    async update(customerId: string, updateData: CustomerUpdateRequest): Promise<Customer> {
        // 获取当前数据（用于审计）
        const currentCustomer = await this.getById(customerId);
        if (!currentCustomer) {
            throw new Error('Customer not found');
        }

        // 数据验证
        await this.validateCustomerData(updateData, customerId);
        
        // 更新客户
        const updatedCustomer = await this.db.updateCustomer(customerId, updateData);
        
        // 清除缓存
        await this.cache.delete(`customer:${customerId}`);
        await this.cache.deletePattern('customer:search:*');
        
        // 记录审计日志
        await this.audit.logCustomerAction('UPDATE', customerId, {
            previous: currentCustomer,
            current: updatedCustomer,
            changes: updateData
        });
        
        return updatedCustomer;
    }

    private async validateCustomerData(data: any, existingCustomerId?: string): Promise<void> {
        // 检查客户代码唯一性
        if (data.customerCode) {
            const existing = await this.db.findCustomerByCode(data.customerCode);
            if (existing && existing.customerId !== existingCustomerId) {
                throw new Error('Customer code already exists');
            }
        }

        // 其他业务规则验证
        if (data.email && !this.isValidEmail(data.email)) {
            throw new Error('Invalid email format');
        }
    }

    private isValidEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
}
```

### Week 7: 数据库集成和API开发

#### 7.1 数据库连接和ORM配置
```typescript
// src/services/DatabaseService.ts
import { ConnectionPool, config } from 'mssql';
import { Customer, CustomerCreateRequest, CustomerUpdateRequest } from '../types/customer';

export class DatabaseService {
    private pool: ConnectionPool;

    constructor() {
        const dbConfig: config = {
            user: process.env.SQL_USER!,
            password: process.env.SQL_PASSWORD!,
            server: process.env.SQL_SERVER!,
            database: process.env.SQL_DATABASE!,
            options: {
                encrypt: true,
                trustServerCertificate: false,
                connectTimeout: 30000,
                requestTimeout: 30000,
            },
            pool: {
                max: 10,
                min: 0,
                idleTimeoutMillis: 30000,
            }
        };

        this.pool = new ConnectionPool(dbConfig);
        this.initializeConnection();
    }

    private async initializeConnection(): Promise<void> {
        try {
            await this.pool.connect();
            console.log('Database connected successfully');
        } catch (error) {
            console.error('Database connection failed:', error);
            throw error;
        }
    }

    async findCustomerById(customerId: string): Promise<Customer | null> {
        try {
            const request = this.pool.request();
            request.input('customerId', customerId);
            
            const result = await request.query(`
                SELECT 
                    c.CustomerId,
                    c.CustomerCode,
                    c.CompanyName,
                    c.ContactPerson,
                    c.Phone,
                    c.Email,
                    c.Address,
                    c.Industry,
                    c.Status,
                    c.CreatedDate,
                    cc.CreditLimit,
                    cc.AvailableCredit,
                    cc.CreditRating,
                    cc.RiskScore
                FROM Customers c
                LEFT JOIN CustomerCredit cc ON c.CustomerId = cc.CustomerId
                WHERE c.CustomerId = @customerId AND c.IsDeleted = 0
            `);

            if (result.recordset.length === 0) {
                return null;
            }

            return this.mapToCustomer(result.recordset[0]);
        } catch (error) {
            console.error('Error finding customer by ID:', error);
            throw error;
        }
    }

    async searchCustomers(query: string, offset: number, limit: number): Promise<Customer[]> {
        try {
            const request = this.pool.request();
            request.input('query', `%${query}%`);
            request.input('offset', offset);
            request.input('limit', limit);
            
            const result = await request.query(`
                SELECT 
                    c.CustomerId,
                    c.CustomerCode,
                    c.CompanyName,
                    c.ContactPerson,
                    c.Phone,
                    c.Email,
                    c.Industry,
                    c.Status,
                    cc.CreditRating,
                    cc.RiskScore
                FROM Customers c
                LEFT JOIN CustomerCredit cc ON c.CustomerId = cc.CustomerId
                WHERE (c.CompanyName LIKE @query OR c.CustomerCode LIKE @query)
                  AND c.IsDeleted = 0
                ORDER BY c.CompanyName
                OFFSET @offset ROWS
                FETCH NEXT @limit ROWS ONLY
            `);

            return result.recordset.map(row => this.mapToCustomer(row));
        } catch (error) {
            console.error('Error searching customers:', error);
            throw error;
        }
    }

    async createCustomer(customerData: CustomerCreateRequest): Promise<Customer> {
        const transaction = this.pool.transaction();
        
        try {
            await transaction.begin();
            
            // 插入客户基本信息
            const customerRequest = transaction.request();
            customerRequest.input('customerCode', customerData.customerCode);
            customerRequest.input('companyName', customerData.companyName);
            customerRequest.input('contactPerson', customerData.contactPerson);
            customerRequest.input('phone', customerData.phone);
            customerRequest.input('email', customerData.email);
            customerRequest.input('address', customerData.address);
            customerRequest.input('industry', customerData.industry);
            customerRequest.input('tenantId', process.env.TENANT_ID);
            
            const customerResult = await customerRequest.query(`
                INSERT INTO Customers (
                    CustomerCode, CompanyName, ContactPerson, 
                    Phone, Email, Address, Industry, TenantId,
                    CreatedDate, CreatedBy, Status
                )
                OUTPUT INSERTED.CustomerId
                VALUES (
                    @customerCode, @companyName, @contactPerson,
                    @phone, @email, @address, @industry, @tenantId,
                    GETUTCDATE(), 'SYSTEM', 'ACTIVE'
                )
            `);
            
            const customerId = customerResult.recordset[0].CustomerId;
            
            // 创建默认信用档案
            const creditRequest = transaction.request();
            creditRequest.input('customerId', customerId);
            creditRequest.input('creditLimit', customerData.initialCreditLimit || 100000);
            creditRequest.input('tenantId', process.env.TENANT_ID);
            
            await creditRequest.query(`
                INSERT INTO CustomerCredit (
                    CustomerId, CreditLimit, AvailableCredit, 
                    CreditRating, RiskScore, TenantId,
                    CreatedDate, ApprovalStatus
                )
                VALUES (
                    @customerId, @creditLimit, @creditLimit,
                    'C', 50, @tenantId,
                    GETUTCDATE(), 'PENDING'
                )
            `);
            
            await transaction.commit();
            
            // 返回完整的客户信息
            return await this.findCustomerById(customerId);
            
        } catch (error) {
            await transaction.rollback();
            console.error('Error creating customer:', error);
            throw error;
        }
    }

    private mapToCustomer(row: any): Customer {
        return {
            customerId: row.CustomerId,
            customerCode: row.CustomerCode,
            companyName: row.CompanyName,
            contactPerson: row.ContactPerson,
            phone: row.Phone,
            email: row.Email,
            address: row.Address,
            industry: row.Industry,
            status: row.Status,
            createdDate: row.CreatedDate,
            credit: {
                creditLimit: row.CreditLimit,
                availableCredit: row.AvailableCredit,
                creditRating: row.CreditRating,
                riskScore: row.RiskScore,
            }
        };
    }
}
```

### Week 8: 功能对等性验证

#### 8.1 功能测试框架
```typescript
// tests/integration/CustomerManagement.test.ts
import { describe, it, expect, beforeAll, afterAll } from '@jest/globals';
import { CustomerService } from '../../src/services/CustomerService';
import { TestDataFactory } from '../helpers/TestDataFactory';
import { DatabaseHelper } from '../helpers/DatabaseHelper';

describe('Customer Management Integration Tests', () => {
    let customerService: CustomerService;
    let testData: any;

    beforeAll(async () => {
        customerService = new CustomerService();
        testData = new TestDataFactory();
        await DatabaseHelper.setupTestData();
    });

    afterAll(async () => {
        await DatabaseHelper.cleanupTestData();
    });

    describe('Customer Search', () => {
        it('should find customers by company name', async () => {
            // Arrange
            const testCustomer = await testData.createTestCustomer({
                companyName: 'Test Manufacturing Ltd'
            });

            // Act
            const result = await customerService.search('Manufacturing');

            // Assert
            expect(result.customers).toHaveLength(1);
            expect(result.customers[0].companyName).toBe('Test Manufacturing Ltd');
            expect(result.total).toBe(1);
        });

        it('should find customers by customer code', async () => {
            // Arrange
            const testCustomer = await testData.createTestCustomer({
                customerCode: 'TEST001'
            });

            // Act
            const result = await customerService.search('TEST001');

            // Assert
            expect(result.customers).toHaveLength(1);
            expect(result.customers[0].customerCode).toBe('TEST001');
        });

        it('should support pagination', async () => {
            // Arrange
            await testData.createMultipleTestCustomers(15);

            // Act
            const page1 = await customerService.search('Test', 1, 10);
            const page2 = await customerService.search('Test', 2, 10);

            // Assert
            expect(page1.customers).toHaveLength(10);
            expect(page2.customers).toHaveLength(5);
            expect(page1.total).toBe(15);
        });
    });

    describe('Customer Details', () => {
        it('should return complete customer information', async () => {
            // Arrange
            const testCustomer = await testData.createTestCustomerWithCredit();

            // Act
            const customer = await customerService.getById(testCustomer.customerId);

            // Assert
            expect(customer).toBeDefined();
            expect(customer.customerId).toBe(testCustomer.customerId);
            expect(customer.credit).toBeDefined();
            expect(customer.credit.creditLimit).toBeGreaterThan(0);
        });

        it('should return null for non-existent customer', async () => {
            // Act
            const customer = await customerService.getById('non-existent-id');

            // Assert
            expect(customer).toBeNull();
        });
    });

    describe('Customer Creation', () => {
        it('should create customer with valid data', async () => {
            // Arrange
            const customerData = {
                customerCode: 'NEW001',
                companyName: 'New Test Company',
                contactPerson: 'John Doe',
                phone: '+1-555-0123',
                email: 'john@newtest.com',
                industry: 'Technology'
            };

            // Act
            const createdCustomer = await customerService.create(customerData);

            // Assert
            expect(createdCustomer.customerId).toBeDefined();
            expect(createdCustomer.customerCode).toBe('NEW001');
            expect(createdCustomer.credit).toBeDefined();
            expect(createdCustomer.credit.creditRating).toBe('C');
        });

        it('should reject duplicate customer code', async () => {
            // Arrange
            await testData.createTestCustomer({ customerCode: 'DUP001' });
            const duplicateData = {
                customerCode: 'DUP001',
                companyName: 'Duplicate Company'
            };

            // Act & Assert
            await expect(customerService.create(duplicateData))
                .rejects.toThrow('Customer code already exists');
        });
    });

    describe('Performance Tests', () => {
        it('customer search should complete within 500ms', async () => {
            // Arrange
            await testData.createMultipleTestCustomers(100);

            // Act
            const startTime = Date.now();
            await customerService.search('Test');
            const endTime = Date.now();

            // Assert
            expect(endTime - startTime).toBeLessThan(500);
        });

        it('customer details should complete within 200ms', async () => {
            // Arrange
            const testCustomer = await testData.createTestCustomer();

            // Act
            const startTime = Date.now();
            await customerService.getById(testCustomer.customerId);
            const endTime = Date.now();

            // Assert
            expect(endTime - startTime).toBeLessThan(200);
        });
    });
});
```

#### 8.2 Legacy vs Azure功能对比测试
```typescript
// tests/comparison/LegacyVsAzure.test.ts
import { describe, it, expect } from '@jest/globals';
import { LegacyCustomerService } from '../legacy/LegacyCustomerService';
import { AzureCustomerService } from '../../src/services/CustomerService';

describe('Legacy vs Azure Functionality Comparison', () => {
    let legacyService: LegacyCustomerService;
    let azureService: AzureCustomerService;

    beforeAll(() => {
        legacyService = new LegacyCustomerService();
        azureService = new AzureCustomerService();
    });

    describe('Customer Search Comparison', () => {
        it('should return equivalent results for same search term', async () => {
            // Arrange
            const searchTerm = 'CUST001';

            // Act
            const legacyResults = await legacyService.searchCustomers(searchTerm);
            const azureResults = await azureService.search(searchTerm);

            // Assert
            expect(azureResults.customers).toHaveLength(legacyResults.length);
            
            // Compare first result details
            if (legacyResults.length > 0) {
                const legacyCustomer = legacyResults[0];
                const azureCustomer = azureResults.customers[0];
                
                expect(azureCustomer.customerCode).toBe(legacyCustomer.customer_code);
                expect(azureCustomer.companyName).toBe(legacyCustomer.company_name);
                expect(azureCustomer.contactPerson).toBe(legacyCustomer.contact_person);
            }
        });
    });

    describe('Performance Comparison', () => {
        it('Azure should be faster than Legacy for customer search', async () => {
            // Legacy timing
            const legacyStart = Date.now();
            await legacyService.searchCustomers('Manufacturing');
            const legacyTime = Date.now() - legacyStart;

            // Azure timing
            const azureStart = Date.now();
            await azureService.search('Manufacturing');
            const azureTime = Date.now() - azureStart;

            // Assert improvement
            expect(azureTime).toBeLessThan(legacyTime);
            console.log(`Performance improvement: ${((legacyTime - azureTime) / legacyTime * 100).toFixed(1)}%`);
        });
    });
});
```

---

## Phase 3: 数据迁移和测试 (Week 9-12)

### Week 9-10: 数据迁移执行
参考 [DATA_MIGRATION.md](./DATA_MIGRATION.md) 详细执行计划

### Week 11: 端到端集成测试

#### 11.1 端到端测试框架
```typescript
// tests/e2e/CustomerJourney.e2e.ts
import { test, expect } from '@playwright/test';

test.describe('Complete Customer Management Journey', () => {
    test.beforeEach(async ({ page }) => {
        // 登录测试用户
        await page.goto('/login');
        await page.fill('[data-testid=username]', 'test@creditcontrol.com');
        await page.fill('[data-testid=password]', 'TestPassword123!');
        await page.click('[data-testid=login-button]');
        
        // 等待主页加载
        await expect(page.locator('[data-testid=dashboard]')).toBeVisible();
    });

    test('Complete customer onboarding workflow', async ({ page }) => {
        // 1. 导航到客户搜索页面
        await page.click('[data-testid=nav-customers]');
        await expect(page.locator('[data-testid=customer-search]')).toBeVisible();

        // 2. 验证搜索功能
        await page.fill('[data-testid=search-input]', 'CUST001');
        await page.click('[data-testid=search-button]');
        
        // 验证搜索结果
        await expect(page.locator('[data-testid=search-results]')).toBeVisible();
        await expect(page.locator('[data-testid=customer-row]').first()).toContainText('CUST001');

        // 3. 查看客户详情
        await page.click('[data-testid=customer-row]').first();
        await expect(page.locator('[data-testid=customer-details]')).toBeVisible();
        
        // 验证客户信息显示
        await expect(page.locator('[data-testid=customer-name]')).toContainText('ABC Manufacturing Ltd');
        await expect(page.locator('[data-testid=credit-rating]')).toContainText('A');

        // 4. 修改信用额度
        await page.click('[data-testid=edit-credit-button]');
        await page.fill('[data-testid=credit-limit-input]', '200000');
        await page.fill('[data-testid=reason-input]', 'Business expansion approved');
        await page.click('[data-testid=save-credit-button]');
        
        // 验证修改成功
        await expect(page.locator('[data-testid=success-message]')).toBeVisible();
        await expect(page.locator('[data-testid=credit-limit]')).toContainText('200,000');

        // 5. 检查审计日志
        await page.click('[data-testid=audit-tab]');
        await expect(page.locator('[data-testid=audit-log]').first()).toContainText('Credit limit modified');
    });

    test('Risk assessment workflow', async ({ page }) => {
        // 导航到风险评估页面
        await page.click('[data-testid=nav-risk]');
        await expect(page.locator('[data-testid=risk-dashboard]')).toBeVisible();

        // 验证风险指标卡片
        await expect(page.locator('[data-testid=high-risk-count]')).toBeVisible();
        await expect(page.locator('[data-testid=total-exposure]')).toBeVisible();

        // 筛选高风险客户
        await page.selectOption('[data-testid=risk-filter]', 'HIGH');
        await page.click('[data-testid=apply-filter]');
        
        // 验证筛选结果
        const riskRows = page.locator('[data-testid=risk-row]');
        await expect(riskRows.first()).toBeVisible();
        
        // 检查风险分数
        const firstRiskScore = await riskRows.first().locator('[data-testid=risk-score]').textContent();
        expect(parseInt(firstRiskScore)).toBeGreaterThan(75);
    });

    test('Reports generation workflow', async ({ page }) => {
        // 导航到报表页面
        await page.click('[data-testid=nav-reports]');
        await expect(page.locator('[data-testid=reports-dashboard]')).toBeVisible();

        // 生成客户报表
        await page.click('[data-testid=customer-report-card]');
        await page.selectOption('[data-testid=date-range]', 'LAST_30_DAYS');
        await page.click('[data-testid=generate-report]');
        
        // 等待报表加载
        await expect(page.locator('[data-testid=report-loading]')).toBeHidden();
        await expect(page.locator('[data-testid=report-chart]')).toBeVisible();
        
        // 验证报表数据
        await expect(page.locator('[data-testid=total-customers]')).toContainText(/\d+/);
        await expect(page.locator('[data-testid=active-customers]')).toContainText(/\d+/);

        // 导出报表
        await page.click('[data-testid=export-button]');
        await page.selectOption('[data-testid=export-format]', 'PDF');
        
        // 等待下载开始
        const downloadPromise = page.waitForEvent('download');
        await page.click('[data-testid=confirm-export]');
        const download = await downloadPromise;
        
        // 验证文件名
        expect(download.suggestedFilename()).toMatch(/customer-report-\d{8}\.pdf/);
    });
});
```

### Week 12: 用户验收测试

#### 12.1 UAT测试计划
```yaml
# UAT测试计划配置
uat_test_plan:
  test_phases:
    - name: "功能验收测试"
      duration: "3天"
      participants: ["业务用户", "系统管理员", "财务人员"]
      test_scenarios:
        - customer_management
        - credit_assessment  
        - risk_monitoring
        - reports_generation
        - system_administration
    
    - name: "性能验收测试"
      duration: "2天" 
      participants: ["技术团队", "业务用户"]
      test_scenarios:
        - load_testing
        - stress_testing
        - concurrent_users
    
    - name: "安全验收测试"
      duration: "2天"
      participants: ["安全团队", "合规团队"]
      test_scenarios:
        - access_control
        - data_protection
        - audit_compliance

  acceptance_criteria:
    functional:
      - name: "功能完整性"
        requirement: "100%业务功能正常运行"
        measurement: "功能测试通过率"
        target: "100%"
      
      - name: "数据准确性"
        requirement: "数据迁移无错误"
        measurement: "数据验证通过率"
        target: "100%"
    
    performance:
      - name: "响应时间"
        requirement: "页面加载时间<2秒"
        measurement: "平均响应时间"
        target: "<2秒"
      
      - name: "并发性能"
        requirement: "支持200并发用户"
        measurement: "并发用户测试"
        target: "200用户"
    
    security:
      - name: "访问控制"
        requirement: "角色权限正确"
        measurement: "权限测试通过率"
        target: "100%"
```

#### 12.2 用户培训计划
```markdown
# 用户培训计划

## 培训目标
- 熟悉新系统界面和操作流程
- 理解功能变更和改进点
- 掌握新增功能的使用方法
- 了解系统监控和报警机制

## 培训内容

### 第1天: 系统概览和基础操作
**时间**: 上午9:00-12:00
**参与者**: 所有用户
**内容**:
- 新系统架构介绍
- 登录和界面导航
- 基础功能演示
- 客户搜索和查看

### 第2天: 核心业务功能
**时间**: 上午9:00-17:00  
**参与者**: 业务用户
**内容**:
- 客户管理操作
- 信用额度管理
- 风险评估功能
- 交易处理流程

### 第3天: 报表和分析功能
**时间**: 上午9:00-15:00
**参与者**: 管理人员
**内容**:
- 报表生成和导出
- 数据分析功能
- 仪表板配置
- 预警系统设置

### 第4天: 系统管理
**时间**: 上午9:00-12:00
**参与者**: 系统管理员
**内容**:
- 用户权限管理
- 系统配置
- 监控和日志
- 故障排除

## 培训方法
- 理论讲解 + 实际操作
- 小组练习和讨论
- 一对一答疑
- 操作手册和视频教程

## 培训评估
- 操作技能测试
- 功能理解评估
- 问题解答能力
- 培训反馈收集
```

---

## Phase 4: 生产部署和运维 (Week 13-16)

### Week 13: 生产环境部署

#### 13.1 生产环境配置
```bash
#!/bin/bash
# 生产环境部署脚本

# 环境变量设置
export ENVIRONMENT="production"
export AZURE_SUBSCRIPTION_ID="your-subscription-id"
export RESOURCE_GROUP="rg-creditcontrol-prod"

# 创建生产资源
echo "Creating production resources..."
az group create --name $RESOURCE_GROUP --location "East US"

# 部署基础设施
echo "Deploying infrastructure..."
az deployment group create \
  --resource-group $RESOURCE_GROUP \
  --template-file infrastructure/production.bicep \
  --parameters @infrastructure/production.parameters.json

# 配置SSL证书
echo "Configuring SSL certificates..."
az webapp config ssl bind \
  --certificate-thumbprint $SSL_THUMBPRINT \
  --ssl-type SNI \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP

# 配置自定义域名
echo "Configuring custom domain..."
az webapp config hostname add \
  --webapp-name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP \
  --hostname "creditcontrol.yourcompany.com"

# 配置监控告警
echo "Setting up monitoring alerts..."
az monitor metrics alert create \
  --name "High CPU Usage" \
  --resource-group $RESOURCE_GROUP \
  --scopes "/subscriptions/$AZURE_SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Web/sites/$WEBAPP_NAME" \
  --condition "avg Percentage CPU > 80" \
  --description "Alert when CPU usage is high"

echo "Production deployment completed!"
```

#### 13.2 数据库生产配置
```sql
-- 生产数据库优化配置
-- 启用查询存储
ALTER DATABASE [creditcontrol-prod] 
SET QUERY_STORE = ON 
(
    OPERATION_MODE = READ_WRITE,
    CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30),
    DATA_FLUSH_INTERVAL_SECONDS = 900,
    MAX_STORAGE_SIZE_MB = 1000,
    INTERVAL_LENGTH_MINUTES = 60
);

-- 配置自动调优
ALTER DATABASE [creditcontrol-prod] 
SET AUTOMATIC_TUNING = AUTO;

-- 创建生产维护计划
EXEC dbo.sp_add_maintenance_plan 
    @plan_name = 'CreditControl_Production_Maintenance',
    @description = 'Daily maintenance for production database',
    @schedule = 'FREQ_DAILY; TIME_030000'; -- 每天凌晨3点

-- 配置备份策略
ALTER DATABASE [creditcontrol-prod]
SET BACKUP_RETENTION = 35; -- 35天短期保留

-- 长期备份配置
ALTER DATABASE [creditcontrol-prod]
ADD LONG_TERM_RETENTION (
    WEEKLY_RETENTION = 'P12W',   -- 12周
    MONTHLY_RETENTION = 'P24M',  -- 24个月
    YEARLY_RETENTION = 'P10Y',   -- 10年
    WEEK_OF_YEAR = 1
);

-- 创建性能监控视图
CREATE VIEW vw_performance_metrics AS
SELECT 
    db_name() as DatabaseName,
    (SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process = 1) as ActiveSessions,
    (SELECT COUNT(*) FROM sys.dm_exec_requests) as ActiveRequests,
    (SELECT AVG(avg_cpu_percent) FROM sys.dm_db_resource_stats WHERE end_time > DATEADD(hour, -1, GETUTCDATE())) as AvgCpuPercent,
    (SELECT AVG(avg_data_io_percent) FROM sys.dm_db_resource_stats WHERE end_time > DATEADD(hour, -1, GETUTCDATE())) as AvgDataIoPercent,
    GETUTCDATE() as MetricTime;
```

### Week 14: 蓝绿切换

#### 14.1 蓝绿部署策略
```yaml
# Azure DevOps蓝绿部署管道
trigger: none

variables:
  azureSubscription: 'Production-Subscription'
  resourceGroupName: 'rg-creditcontrol-prod'
  webAppName: 'app-creditcontrol-prod'
  
stages:
- stage: Deploy_Green
  displayName: 'Deploy to Green Environment'
  jobs:
  - job: DeployGreen
    displayName: 'Deploy Green Slot'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: AzureWebApp@1
      displayName: 'Deploy to Green Slot'
      inputs:
        azureSubscription: $(azureSubscription)
        appName: $(webAppName)
        deployToSlotOrASE: true
        slotName: 'green'
        package: '$(Pipeline.Workspace)/drop/*.zip'
    
    - task: AzureAppServiceManage@0
      displayName: 'Warm up Green Slot'
      inputs:
        azureSubscription: $(azureSubscription)
        action: 'Start Azure App Service'
        webAppName: $(webAppName)
        specifySlotOrASE: true
        slot: 'green'

- stage: Test_Green
  displayName: 'Test Green Environment'
  dependsOn: Deploy_Green
  jobs:
  - job: TestGreen
    displayName: 'Run Smoke Tests'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: NodeTool@0
      inputs:
        versionSpec: '18.x'
    
    - script: |
        npm install
        npm run test:smoke -- --environment=green
      displayName: 'Run Smoke Tests on Green'
    
    - task: PublishTestResults@2
      inputs:
        testResultsFormat: 'JUnit'
        testResultsFiles: 'test-results/smoke-tests.xml'

- stage: Switch_Traffic
  displayName: 'Switch Traffic to Green'
  dependsOn: Test_Green
  condition: succeeded()
  jobs:
  - deployment: SwitchProduction
    displayName: 'Switch to Green'
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: AzureAppServiceManage@0
            displayName: 'Swap Slots'
            inputs:
              azureSubscription: $(azureSubscription)
              webAppName: $(webAppName)
              action: 'Swap Slots'
              sourceSlot: 'green'
              targetSlot: 'production'
          
          - script: |
              echo "Traffic switched to green environment"
              echo "Monitoring for 10 minutes..."
              sleep 600
            displayName: 'Monitor Switch'
          
          - task: AzureAppServiceManage@0
            displayName: 'Health Check'
            inputs:
              azureSubscription: $(azureSubscription)
              action: 'Check App Service Health'
              webAppName: $(webAppName)

- stage: Rollback_If_Needed
  displayName: 'Rollback if Issues Detected'
  dependsOn: Switch_Traffic
  condition: failed()
  jobs:
  - job: Rollback
    displayName: 'Rollback to Blue'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: AzureAppServiceManage@0
      displayName: 'Rollback Swap'
      inputs:
        azureSubscription: $(azureSubscription)
        webAppName: $(webAppName)
        action: 'Swap Slots'
        sourceSlot: 'production'
        targetSlot: 'green'
```

#### 14.2 切换验证脚本
```javascript
// scripts/validate-deployment.js
const axios = require('axios');
const { performance } = require('perf_hooks');

class DeploymentValidator {
    constructor(baseUrl, apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.results = {
            healthCheck: false,
            performanceCheck: false,
            functionalCheck: false,
            dataIntegrityCheck: false
        };
    }

    async validateDeployment() {
        console.log('Starting deployment validation...');
        
        try {
            await this.runHealthCheck();
            await this.runPerformanceCheck();
            await this.runFunctionalCheck();
            await this.runDataIntegrityCheck();
            
            const allPassed = Object.values(this.results).every(result => result === true);
            
            console.log('Validation Results:', this.results);
            
            if (allPassed) {
                console.log('✅ All validation checks passed!');
                process.exit(0);
            } else {
                console.log('❌ Some validation checks failed!');
                process.exit(1);
            }
            
        } catch (error) {
            console.error('Validation failed with error:', error.message);
            process.exit(1);
        }
    }

    async runHealthCheck() {
        console.log('Running health check...');
        
        try {
            const response = await axios.get(`${this.baseUrl}/api/health`, {
                headers: { 'Authorization': `Bearer ${this.apiKey}` },
                timeout: 10000
            });
            
            if (response.status === 200 && response.data.status === 'healthy') {
                console.log('✅ Health check passed');
                this.results.healthCheck = true;
            } else {
                console.log('❌ Health check failed');
            }
        } catch (error) {
            console.log('❌ Health check failed:', error.message);
        }
    }

    async runPerformanceCheck() {
        console.log('Running performance check...');
        
        try {
            const start = performance.now();
            
            await axios.get(`${this.baseUrl}/api/customers/search?q=test`, {
                headers: { 'Authorization': `Bearer ${this.apiKey}` },
                timeout: 5000
            });
            
            const end = performance.now();
            const responseTime = end - start;
            
            if (responseTime < 2000) { // 2秒阈值
                console.log(`✅ Performance check passed (${responseTime.toFixed(2)}ms)`);
                this.results.performanceCheck = true;
            } else {
                console.log(`❌ Performance check failed (${responseTime.toFixed(2)}ms > 2000ms)`);
            }
        } catch (error) {
            console.log('❌ Performance check failed:', error.message);
        }
    }

    async runFunctionalCheck() {
        console.log('Running functional check...');
        
        try {
            // 测试关键业务功能
            const testCases = [
                { name: 'Customer Search', endpoint: '/api/customers/search?q=CUST001' },
                { name: 'Customer Details', endpoint: '/api/customers/test-customer-id' },
                { name: 'Risk Assessment', endpoint: '/api/risk/dashboard' }
            ];
            
            let passedTests = 0;
            
            for (const testCase of testCases) {
                try {
                    const response = await axios.get(`${this.baseUrl}${testCase.endpoint}`, {
                        headers: { 'Authorization': `Bearer ${this.apiKey}` },
                        timeout: 5000
                    });
                    
                    if (response.status === 200) {
                        console.log(`✅ ${testCase.name} test passed`);
                        passedTests++;
                    }
                } catch (error) {
                    console.log(`❌ ${testCase.name} test failed`);
                }
            }
            
            if (passedTests === testCases.length) {
                this.results.functionalCheck = true;
            }
            
        } catch (error) {
            console.log('❌ Functional check failed:', error.message);
        }
    }

    async runDataIntegrityCheck() {
        console.log('Running data integrity check...');
        
        try {
            const response = await axios.get(`${this.baseUrl}/api/admin/data-validation`, {
                headers: { 'Authorization': `Bearer ${this.apiKey}` },
                timeout: 30000
            });
            
            if (response.status === 200 && response.data.isValid) {
                console.log('✅ Data integrity check passed');
                this.results.dataIntegrityCheck = true;
            } else {
                console.log('❌ Data integrity check failed');
                if (response.data.errors) {
                    console.log('Errors:', response.data.errors);
                }
            }
        } catch (error) {
            console.log('❌ Data integrity check failed:', error.message);
        }
    }
}

// 运行验证
const validator = new DeploymentValidator(
    process.env.DEPLOYMENT_URL || 'https://creditcontrol.yourcompany.com',
    process.env.API_KEY
);

validator.validateDeployment();
```

### Week 15-16: 运维优化和项目收尾

#### 15.1 运维监控完善
```yaml
# Azure Monitor告警规则配置
alert_rules:
  - name: "High CPU Usage"
    description: "CPU使用率超过80%"
    condition: "avg Percentage CPU > 80"
    timeWindow: "PT5M"
    frequency: "PT1M"
    severity: 2
    actions:
      - actionGroup: "critical-alerts"
      - webhook: "https://hooks.slack.com/services/..."

  - name: "Database Connection Failures"
    description: "数据库连接失败"
    condition: "count connection_failed > 5"
    timeWindow: "PT5M"
    frequency: "PT1M"
    severity: 1
    actions:
      - actionGroup: "critical-alerts"
      - sms: "+1-555-0123"

  - name: "Low Available Memory"
    description: "可用内存低于20%"
    condition: "avg Available Memory Bytes < 0.2"
    timeWindow: "PT10M"
    frequency: "PT5M"
    severity: 3
    actions:
      - actionGroup: "warning-alerts"

  - name: "Application Errors"
    description: "应用程序错误率过高"
    condition: "count exceptions > 10"
    timeWindow: "PT15M"
    frequency: "PT5M"
    severity: 2
    actions:
      - actionGroup: "dev-team-alerts"
```

#### 15.2 性能优化和调优
```sql
-- 数据库性能优化
-- 1. 索引优化
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Transactions_Performance')
CREATE INDEX IX_Transactions_Performance 
ON Transactions (CustomerId, TransactionDate, Status)
INCLUDE (Amount, TransactionType);

-- 2. 统计信息更新
UPDATE STATISTICS Customers WITH FULLSCAN;
UPDATE STATISTICS Transactions WITH FULLSCAN;
UPDATE STATISTICS CustomerCredit WITH FULLSCAN;

-- 3. 查询计划缓存清理
DBCC FREEPROCCACHE;

-- 4. 创建性能监控存储过程
CREATE PROCEDURE sp_monitor_performance
AS
BEGIN
    SELECT 
        'Database Performance' as MetricType,
        db_name() as DatabaseName,
        (SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process = 1) as ActiveSessions,
        (SELECT COUNT(*) FROM sys.dm_exec_requests) as ActiveRequests,
        (SELECT AVG(wait_time_ms) FROM sys.dm_os_wait_stats WHERE wait_type NOT LIKE '%SLEEP%') as AvgWaitTime,
        GETUTCDATE() as CheckTime;
        
    -- 慢查询监控
    SELECT TOP 10
        'Slow Queries' as MetricType,
        q.query_id,
        qt.query_sql_text,
        rs.avg_duration / 1000.0 as avg_duration_ms,
        rs.avg_cpu_time / 1000.0 as avg_cpu_time_ms,
        rs.count_executions
    FROM sys.query_store_query q
    JOIN sys.query_store_query_text qt ON q.query_text_id = qt.query_text_id
    JOIN sys.query_store_runtime_stats rs ON q.query_id = rs.query_id
    WHERE rs.last_execution_time > DATEADD(hour, -1, GETUTCDATE())
    ORDER BY rs.avg_duration DESC;
END;
```

#### 15.3 项目交付文档
```markdown
# 项目交付清单

## 1. 技术交付物
- ✅ 前端应用 (React + TypeScript)
- ✅ 后端微服务 (Azure Functions)
- ✅ 数据库架构 (Azure SQL Database)
- ✅ 基础设施代码 (Terraform)
- ✅ CI/CD流水线 (Azure DevOps)
- ✅ 监控告警 (Azure Monitor)
- ✅ 安全配置 (Azure AD B2C + Key Vault)

## 2. 文档交付物
- ✅ 系统架构文档
- ✅ 用户操作手册
- ✅ 系统管理员指南
- ✅ 故障排除手册
- ✅ 灾难恢复计划
- ✅ 安全运维指南

## 3. 测试交付物
- ✅ 单元测试覆盖率报告 (>90%)
- ✅ 集成测试结果
- ✅ 端到端测试报告
- ✅ 性能测试报告
- ✅ 安全测试报告
- ✅ UAT验收报告

## 4. 运维交付物
- ✅ 监控仪表板配置
- ✅ 告警规则配置
- ✅ 备份恢复策略
- ✅ 性能基线数据
- ✅ 容量规划建议
- ✅ 成本优化建议

## 5. 知识转移
- ✅ 技术团队培训完成
- ✅ 运维团队培训完成
- ✅ 业务用户培训完成
- ✅ 管理层汇报完成

## 6. 项目收尾
- ✅ 项目总结报告
- ✅ 经验教训文档
- ✅ 后续改进建议
- ✅ 项目成果展示
```

---

## 风险管理和应急预案

### 风险识别和缓解

| 风险类型 | 风险描述 | 概率 | 影响 | 缓解措施 |
|----------|----------|------|------|----------|
| **技术风险** | 数据迁移失败 | 中 | 高 | 完整备份+回滚计划 |
| **业务风险** | 用户接受度低 | 低 | 中 | 充分培训+渐进切换 |
| **性能风险** | 系统响应变慢 | 中 | 中 | 性能测试+优化调优 |
| **安全风险** | 数据泄露 | 低 | 高 | 多层安全防护+审计 |

### 应急响应计划

```yaml
emergency_response:
  system_outage:
    detection:
      - automated_monitoring: "Azure Monitor alerts"
      - user_reports: "Support ticket system"
      - health_checks: "Automated health endpoints"
    
    response_team:
      - on_call_engineer: "Primary escalation"
      - database_admin: "Data layer issues"
      - security_team: "Security incidents"
      - business_lead: "Business impact assessment"
    
    response_steps:
      1. "Acknowledge incident within 5 minutes"
      2. "Assess impact and severity"
      3. "Initiate communication plan"
      4. "Execute recovery procedures"
      5. "Monitor system stability"
      6. "Conduct post-incident review"

  rollback_procedures:
    triggers:
      - "Critical functionality failure"
      - "Data corruption detected"
      - "Security breach identified"
      - "Performance degradation >50%"
    
    rollback_steps:
      1. "Stop new deployments"
      2. "Switch traffic to previous version"
      3. "Verify rollback success"
      4. "Notify stakeholders"
      5. "Investigate root cause"
```

---

**文档版本**: 1.0  
**最后更新**: 2025-01-15  
**项目经理**: 实施团队
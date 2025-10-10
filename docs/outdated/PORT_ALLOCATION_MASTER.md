# 🔌 **项目端口分配主文档** 

## 🚨 **重要声明**
**此文档为项目端口分配的最高级标准文档，端口配置已确定并锁定，严禁任何修改！**

---

## 📊 **端口使用现状检查**

### **当前系统端口占用情况** (检查时间: 2025-01-09)
```bash
# 已占用端口
tcp        0      0 0.0.0.0:5432            0.0.0.0:*               LISTEN     # PostgreSQL
tcp        0      0 127.0.0.1:6379          0.0.0.0:*               LISTEN     # Redis
tcp6       0      0 ::1:6379                :::*                    LISTEN     # Redis IPv6
tcp6       0      0 :::5432                 :::*                    LISTEN     # PostgreSQL IPv6

# 微服务端口检查结果
端口8080-8086, 3000 当前未使用 ✅ 可用
```

### **端口分配状态总览**
- ✅ **PostgreSQL (5432)**: 已占用 - 现有数据库服务
- ✅ **Redis (6379)**: 已占用 - 现有缓存服务  
- 🟢 **前端端口 (3000)**: 可用 - 预留给React开发服务器
- 🟢 **API网关 (8080)**: 可用 - 预留给Nginx网关
- 🟢 **微服务端口 (8081-8086)**: 全部可用 - 预留给微服务

---

## 🏗️ **标准端口分配表**

### **Phase 1: 容器化开发阶段端口映射**

| 🎯 **服务名称** | 🔌 **外部端口** | 📦 **容器内端口** | 📋 **服务描述** | 🔒 **状态** |
|-----------------|----------------|------------------|----------------|-------------|
| **前端层** |
| React Frontend | `3000` | `3000` | 开发环境前端服务 | 🔒 锁定 |
| **网关层** |
| Nginx Gateway | `8080` | `80` | API网关和反向代理 | 🔒 锁定 |
| **微服务层** |
| Customer Service | `8081` | `8080` | 客户管理微服务 | 🔒 锁定 |
| Credit Service | `8082` | `8080` | 信用管理微服务 | 🔒 锁定 |
| Risk Service | `8083` | `8080` | 风险评估微服务 | 🔒 锁定 |
| Payment Service | `8084` | `8080` | 付款处理微服务 | 🔒 锁定 |
| Report Service | `8085` | `8080` | 报表生成微服务 | 🔒 锁定 |
| Notification Service | `8086` | `8080` | 通知推送微服务 | 🔒 锁定 |
| **数据层** |
| PostgreSQL Database | `5432` | `5432` | 主数据库 (现有) | 🔒 锁定 |
| Redis Cache | `6379` | `6379` | 缓存服务 (现有) | 🔒 锁定 |

---

## 🌐 **服务访问URL标准**

### **开发环境访问地址**
```bash
# 前端应用
http://localhost:3000                    # React开发服务器

# API网关
http://localhost:8080                    # Nginx API网关
http://localhost:8080/api/v1             # 统一API入口

# 微服务直接访问 (开发调试用)
http://localhost:8081/api/v1/customers   # 客户服务
http://localhost:8082/api/v1/credit      # 信用服务  
http://localhost:8083/api/v1/risk        # 风险服务
http://localhost:8084/api/v1/payments    # 付款服务
http://localhost:8085/api/v1/reports     # 报表服务
http://localhost:8086/api/v1/notifications # 通知服务

# 数据库连接
postgresql://creditapp:creditapp@35.77.54.203:5432/creditcontrol
redis://localhost:6379

# 健康检查端点
http://localhost:8081/actuator/health    # Customer Service
http://localhost:8082/actuator/health    # Credit Service
http://localhost:8083/actuator/health    # Risk Service
http://localhost:8084/actuator/health    # Payment Service
http://localhost:8085/actuator/health    # Report Service
http://localhost:8086/actuator/health    # Notification Service
```

---

## 📝 **Podman容器编排配置**

### **podman-compose.yml 端口配置标准**
```yaml
version: '3.8'

services:
  # 前端服务
  frontend:
    build: ./frontend
    ports:
      - "3000:3000"    # 🔒 锁定端口
    environment:
      - REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
    depends_on:
      - gateway

  # API网关
  gateway:
    image: nginx:alpine
    ports:
      - "8080:80"      # 🔒 锁定端口
    volumes:
      - ./infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - customer-service
      - credit-service
      - risk-service
      - payment-service
      - report-service
      - notification-service

  # 客户管理微服务
  customer-service:
    build: ./backend/customer-service
    ports:
      - "8081:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
      - DB_HOST=35.77.54.203
      - DB_PORT=5432
      - DB_NAME=creditcontrol
      - DB_USERNAME=creditapp
      - DB_PASSWORD=creditapp
      - REDIS_HOST=localhost
      - REDIS_PORT=6379
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # 信用管理微服务
  credit-service:
    build: ./backend/credit-service
    ports:
      - "8082:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
      - DB_HOST=35.77.54.203
      - DB_PORT=5432
    depends_on:
      - customer-service
    restart: unless-stopped

  # 风险评估微服务
  risk-service:
    build: ./backend/risk-service
    ports:
      - "8083:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
    restart: unless-stopped

  # 付款处理微服务
  payment-service:
    build: ./backend/payment-service
    ports:
      - "8084:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
    restart: unless-stopped

  # 报表生成微服务
  report-service:
    build: ./backend/report-service
    ports:
      - "8085:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
    restart: unless-stopped

  # 通知推送微服务
  notification-service:
    build: ./backend/notification-service
    ports:
      - "8086:8080"    # 🔒 锁定端口
    environment:
      - SPRING_PROFILES_ACTIVE=development
      - SERVER_PORT=8080
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

---

## 🔧 **Nginx网关配置标准**

### **nginx.conf 端口路由配置**
```nginx
# /infrastructure/nginx/nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream customer-service {
        server customer-service:8080;  # 容器内部端口
    }
    
    upstream credit-service {
        server credit-service:8080;    # 容器内部端口
    }
    
    upstream risk-service {
        server risk-service:8080;      # 容器内部端口
    }
    
    upstream payment-service {
        server payment-service:8080;   # 容器内部端口
    }
    
    upstream report-service {
        server report-service:8080;    # 容器内部端口
    }
    
    upstream notification-service {
        server notification-service:8080; # 容器内部端口
    }

    server {
        listen 80;  # 容器内端口，映射到外部8080
        server_name localhost;

        # 客户服务路由
        location /api/v1/customers {
            proxy_pass http://customer-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # 信用服务路由
        location /api/v1/credit {
            proxy_pass http://credit-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # 风险服务路由
        location /api/v1/risk {
            proxy_pass http://risk-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # 付款服务路由
        location /api/v1/payments {
            proxy_pass http://payment-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # 报表服务路由
        location /api/v1/reports {
            proxy_pass http://report-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # 通知服务路由
        location /api/v1/notifications {
            proxy_pass http://notification-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # 健康检查路由
        location /health {
            return 200 "Nginx Gateway OK";
            add_header Content-Type text/plain;
        }
    }
}
```

---

## 🚀 **Phase 2: Azure云端端口映射**

### **Azure迁移后服务访问**
| 🎯 **Azure服务** | 🌐 **访问方式** | 📋 **说明** |
|------------------|----------------|-------------|
| Static Web Apps | `https://creditcontrol.azurestaticapps.net` | 前端应用 |
| API Management | `https://creditcontrol-api.azure-api.net` | API网关 |
| Azure Functions | 内部通信 | 微服务 (不直接暴露) |
| Azure SQL Database | 内部连接字符串 | 数据库 |
| Azure Cache for Redis | 内部连接字符串 | 缓存 |

**注意**: Azure环境中微服务不再使用固定端口，而是通过Azure服务发现和内部DNS进行通信。

---

## ⚠️ **端口使用规则和限制**

### **🔒 强制规则**
1. **端口锁定**: 上述端口分配表中的所有端口配置**严禁修改**
2. **唯一性**: 每个服务必须使用指定的唯一端口，不得冲突
3. **一致性**: 开发、测试、生产环境必须使用相同的端口映射逻辑
4. **文档同步**: 任何配置文件中的端口必须与此文档保持一致

### **✅ 允许操作**
1. **环境变量**: 可通过环境变量配置，但默认值必须符合此文档
2. **健康检查**: 可添加健康检查端点，但不得占用主服务端口
3. **调试端口**: 可临时使用其他端口进行调试，但不得提交到代码库

### **❌ 禁止操作**
1. **修改端口**: 禁止修改任何已分配的端口号
2. **端口复用**: 禁止多个服务共享同一端口
3. **随意分配**: 禁止在未经确认的情况下分配新端口
4. **文档不一致**: 禁止配置文件与此文档不一致

---

## 🔍 **端口冲突检查脚本**

### **自动化端口检查**
```bash
#!/bin/bash
# scripts/check-ports.sh

echo "🔍 检查端口分配冲突..."

# 定义标准端口列表
REQUIRED_PORTS=(3000 8080 8081 8082 8083 8084 8085 8086)
OCCUPIED_PORTS=(5432 6379)  # 已知占用端口

echo "📊 当前端口使用情况:"
for port in "${REQUIRED_PORTS[@]}"; do
    if netstat -tuln | grep -q ":$port "; then
        echo "❌ 端口 $port 已被占用"
        netstat -tuln | grep ":$port "
    else
        echo "✅ 端口 $port 可用"
    fi
done

echo ""
echo "🔒 已知占用端口 (正常):"
for port in "${OCCUPIED_PORTS[@]}"; do
    if netstat -tuln | grep -q ":$port "; then
        echo "🟢 端口 $port 正常占用 (数据库/缓存服务)"
    else
        echo "🟡 端口 $port 未占用 (服务可能未启动)"
    fi
done

echo ""
echo "🎯 端口分配验证完成"
```

### **配置文件验证脚本**
```bash
#!/bin/bash
# scripts/validate-port-config.sh

echo "📝 验证配置文件端口一致性..."

# 检查podman-compose.yml
if grep -q "8081:8080" podman-compose.yml; then
    echo "✅ Customer Service 端口配置正确"
else
    echo "❌ Customer Service 端口配置错误"
fi

if grep -q "8082:8080" podman-compose.yml; then
    echo "✅ Credit Service 端口配置正确"
else
    echo "❌ Credit Service 端口配置错误"
fi

# 检查Spring Boot配置
for service in customer-service credit-service risk-service payment-service report-service notification-service; do
    config_file="backend/$service/src/main/resources/application.yml"
    if [ -f "$config_file" ]; then
        if grep -q "port: 8080" "$config_file"; then
            echo "✅ $service 内部端口配置正确"
        else
            echo "❌ $service 内部端口配置错误"
        fi
    fi
done

echo "🎯 配置文件验证完成"
```

---

## 📚 **相关文档引用**

### **端口配置相关文档**
- `docs/ARCHITECTURE.md` - 第104-165行：标准端口分配方案
- `docs/MIGRATION_PLAN.md` - 第73-98行：容器化架构端口映射
- `docs/IMPLEMENTATION.md` - 第200-255行：Docker Compose端口配置
- `docs/PROGRESS_TRACKING.md` - 第225-244行：Podman容器端口配置

### **端口使用统计**
```
总端口数: 10个
├── 前端端口: 1个 (3000)
├── 网关端口: 1个 (8080)  
├── 微服务端口: 6个 (8081-8086)
└── 数据层端口: 2个 (5432, 6379)

端口范围分布:
├── 3000-3999: 前端服务
├── 5000-5999: 数据库服务
├── 6000-6999: 缓存服务
└── 8000-8999: 后端微服务
```

---

## 🔐 **安全考量**

### **端口安全配置**
1. **防火墙规则**: 生产环境仅开放80/443端口对外访问
2. **内部通信**: 微服务间通信使用内部网络，不暴露端口
3. **数据库访问**: 数据库端口仅允许微服务网络访问
4. **监控端口**: 健康检查端点需要适当的访问控制

### **端口监控**
- 持续监控所有分配端口的使用状态
- 异常端口占用立即告警
- 定期审查端口分配的合理性

---

## 📅 **变更历史**

| 日期 | 版本 | 变更内容 | 变更人 |
|------|------|---------|--------|
| 2025-01-09 | 1.0 | 初始端口分配方案确定 | 开发团队 |
| | | 端口可用性验证完成 | |
| | | 锁定所有端口配置 | |

---

**⚠️ 最终声明**: 本文档为项目端口分配的唯一权威文档，所有端口配置已确定并锁定。任何对端口的修改需求必须通过正式变更流程，经项目负责人批准后才能实施。

**📞 联系方式**: 如有端口相关问题，请联系项目架构师进行咨询。

---

**文档版本**: 1.0  
**最后更新**: 2025-01-09  
**文档状态**: 🔒 **锁定** - 禁止修改
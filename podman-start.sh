#!/bin/bash
# Credit Control System - Podman Start Script
# 使用原生 Podman 命令启动所有服务

set -e

echo "🚀 Credit Control System - 启动所有服务"
echo "=================================================="
echo ""

# 数据库配置
DB_HOST="${DB_HOST:-35.77.54.203}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-creditcontrol}"
DB_USERNAME="${DB_USERNAME:-creditapp}"
DB_PASSWORD="${DB_PASSWORD:-secure123}"

echo "📡 数据库配置:"
echo "   主机: $DB_HOST:$DB_PORT"
echo "   数据库: $DB_NAME"
echo "   用户: $DB_USERNAME"
echo ""

# 检查网络是否存在
if ! podman network exists creditcontrol-network; then
    echo "🌐 创建 Podman 网络..."
    podman network create creditcontrol-network
fi

echo "🔄 启动 Redis 缓存..."
podman run -d \
    --name creditcontrol-redis \
    --network creditcontrol-network \
    -p 6379:6379 \
    --restart unless-stopped \
    docker.io/library/redis:7-alpine \
    2>/dev/null || echo "   ⚠️  Redis 容器已存在，跳过..."

echo "   ✅ Redis 启动完成"
echo ""

echo "🚀 启动微服务..."
echo ""

# Customer Service
echo "📦 启动 Customer Service (端口 8081)..."
podman run -d \
    --name customer-service \
    --network creditcontrol-network \
    -p 8081:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    -e REDIS_HOST=creditcontrol-redis \
    -e REDIS_PORT=6379 \
    --restart unless-stopped \
    creditcontrol/customer-service:latest \
    2>/dev/null || echo "   ⚠️  Customer Service 容器已存在，跳过..."

# Credit Service
echo "📦 启动 Credit Service (端口 8082)..."
podman run -d \
    --name credit-service \
    --network creditcontrol-network \
    -p 8082:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    -e REDIS_HOST=creditcontrol-redis \
    -e REDIS_PORT=6379 \
    --restart unless-stopped \
    creditcontrol/credit-service:latest \
    2>/dev/null || echo "   ⚠️  Credit Service 容器已存在，跳过..."

# Risk Service
echo "📦 启动 Risk Service (端口 8083)..."
podman run -d \
    --name risk-service \
    --network creditcontrol-network \
    -p 8083:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    --restart unless-stopped \
    creditcontrol/risk-service:latest \
    2>/dev/null || echo "   ⚠️  Risk Service 容器已存在，跳过..."

# Payment Service
echo "📦 启动 Payment Service (端口 8084)..."
podman run -d \
    --name payment-service \
    --network creditcontrol-network \
    -p 8084:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    --restart unless-stopped \
    creditcontrol/payment-service:latest \
    2>/dev/null || echo "   ⚠️  Payment Service 容器已存在，跳过..."

# Report Service
echo "📦 启动 Report Service (端口 8085)..."
podman run -d \
    --name report-service \
    --network creditcontrol-network \
    -p 8085:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    --restart unless-stopped \
    creditcontrol/report-service:latest \
    2>/dev/null || echo "   ⚠️  Report Service 容器已存在，跳过..."

# Notification Service
echo "📦 启动 Notification Service (端口 8086)..."
podman run -d \
    --name notification-service \
    --network creditcontrol-network \
    -p 8086:8080 \
    -e SPRING_PROFILES_ACTIVE=development \
    -e DB_HOST=$DB_HOST \
    -e DB_PORT=$DB_PORT \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USERNAME \
    -e DB_PASSWORD=$DB_PASSWORD \
    --restart unless-stopped \
    creditcontrol/notification-service:latest \
    2>/dev/null || echo "   ⚠️  Notification Service 容器已存在，跳过..."

echo ""
echo "🌐 启动 Nginx API 网关 (端口 8080)..."
podman run -d \
    --name api-gateway \
    --network creditcontrol-network \
    -p 8080:80 \
    -v ./backend/infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro,Z \
    --restart unless-stopped \
    docker.io/library/nginx:alpine \
    2>/dev/null || echo "   ⚠️  API Gateway 容器已存在，跳过..."

echo ""
echo "⏳ 等待服务启动 (30秒)..."
sleep 30

echo ""
echo "🎉 所有服务启动完成！"
echo ""
echo "📊 运行中的容器:"
podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🔗 服务访问地址:"
echo "   API 网关:        http://localhost:8080"
echo "   网关健康检查:     http://localhost:8080/health"
echo "   Customer Service: http://localhost:8081/actuator/health"
echo "   Credit Service:   http://localhost:8082/actuator/health"
echo "   Risk Service:     http://localhost:8083/actuator/health"
echo "   Payment Service:  http://localhost:8084/actuator/health"
echo "   Report Service:   http://localhost:8085/actuator/health"
echo "   Notification Service: http://localhost:8086/actuator/health"
echo ""
echo "💡 查看日志: podman logs -f <container-name>"
echo "💡 停止服务: ./podman-stop.sh"

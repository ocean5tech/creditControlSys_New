#!/bin/bash
# Credit Control System - Podman Build Script
# 使用原生 Podman 命令构建所有微服务

set -e

echo "🚀 Credit Control System - 使用 Podman 构建微服务"
echo "=================================================="
echo ""

# 切换到项目根目录
cd "$(dirname "$0")"

# 服务列表
SERVICES=(
    "customer-service"
    "credit-service"
    "risk-service"
    "payment-service"
    "report-service"
    "notification-service"
)

# Maven 镜像
MAVEN_IMAGE="docker.io/library/maven:3.9-eclipse-temurin-17-alpine"

echo "📦 步骤 1/2: 使用 Maven 容器编译所有微服务..."
echo ""

for service in "${SERVICES[@]}"; do
    echo "🔨 编译 $service..."

    podman run --rm \
        -v ./backend/$service:/app:Z \
        -v maven-repo:/root/.m2:Z \
        -w /app \
        $MAVEN_IMAGE \
        mvn clean package -DskipTests -q

    if [ $? -eq 0 ]; then
        echo "   ✅ $service 编译成功"
    else
        echo "   ❌ $service 编译失败"
        exit 1
    fi
    echo ""
done

echo ""
echo "📦 步骤 2/2: 构建 Docker 镜像..."
echo ""

for service in "${SERVICES[@]}"; do
    echo "🏗️  构建 $service 镜像..."

    podman build -t creditcontrol/$service:latest ./backend/$service -q

    if [ $? -eq 0 ]; then
        echo "   ✅ $service 镜像构建成功"
    else
        echo "   ❌ $service 镜像构建失败"
        exit 1
    fi
    echo ""
done

echo ""
echo "🎉 所有微服务构建完成！"
echo ""
echo "📊 构建的镜像:"
podman images | grep creditcontrol
echo ""
echo "💡 下一步: 运行 ./podman-start.sh 启动所有服务"

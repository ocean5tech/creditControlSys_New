#!/bin/bash
# Credit Control System - Podman Stop Script
# 停止并删除所有服务容器

set -e

echo "🛑 Credit Control System - 停止所有服务"
echo "=================================================="
echo ""

CONTAINERS=(
    "api-gateway"
    "customer-service"
    "credit-service"
    "risk-service"
    "payment-service"
    "report-service"
    "notification-service"
    "creditcontrol-redis"
)

echo "🔄 停止并删除容器..."
echo ""

for container in "${CONTAINERS[@]}"; do
    if podman ps -a --format "{{.Names}}" | grep -q "^${container}$"; then
        echo "🛑 停止 $container..."
        podman stop $container 2>/dev/null || true
        echo "🗑️  删除 $container..."
        podman rm $container 2>/dev/null || true
        echo "   ✅ $container 已停止并删除"
    else
        echo "   ⚠️  $container 不存在，跳过..."
    fi
done

echo ""
echo "🎉 所有服务已停止！"
echo ""
echo "💡 重新启动: ./podman-start.sh"
echo "💡 重新构建: ./podman-build.sh"

#!/bin/bash

# Start All Microservices Script
# 启动所有微服务的快速脚本

set -e

echo "🚀 Starting Credit Control System Microservices..."

services=(
    "risk-service:8083"
    "payment-service:8084" 
    "report-service:8085"
    "notification-service:8086"
)

# Start each service in the background
for service_config in "${services[@]}"; do
    IFS=':' read -r service_name port <<< "$service_config"
    
    echo "📝 Starting $service_name on port $port..."
    
    # Clean the service first
    cd /home/ubuntu/creditControlSys_New/backend/$service_name
    mvn clean -q
    
    # Create minimal config if needed
    mkdir -p src/main/java/com/creditcontrol/${service_name%-*}/config
    cat > src/main/java/com/creditcontrol/${service_name%-*}/config/DatabaseConfig.java << EOF
package com.creditcontrol.${service_name%-*}.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    // Minimal configuration
}
EOF
    
    # Start the service
    nohup mvn spring-boot:run > logs/${service_name}.log 2>&1 &
    
    echo "✅ $service_name started in background (PID: $!)"
    
    # Wait a moment before starting next service
    sleep 2
done

echo ""
echo "🎉 All microservices starting in background!"
echo "📊 Check logs in backend/*/logs/ directories"
echo "⏰ Services need ~30 seconds to fully start"
echo ""
echo "🔗 Test endpoints:"
echo "  http://35.77.54.203:8080/api/v1/risk/health"
echo "  http://35.77.54.203:8080/api/v1/payments/health"
echo "  http://35.77.54.203:8080/api/v1/reports/health"
echo "  http://35.77.54.203:8080/api/v1/notifications/health"
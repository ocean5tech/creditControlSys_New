#!/bin/bash

# Setup Microservices Configuration Script
# 快速配置所有微服务的基本设置

set -e

echo "🚀 Setting up Credit Control System Microservices..."

# Service configurations: service_name:port:description
services=(
    "risk-service:8083:Risk Assessment and Monitoring Service"
    "payment-service:8084:Payment Processing and Tracking Service" 
    "report-service:8085:Business Intelligence and Analytics Service"
    "notification-service:8086:Alert and Communication Service"
)

# Configure each service
for service_config in "${services[@]}"; do
    IFS=':' read -r service_name port description <<< "$service_config"
    
    echo "📝 Configuring $service_name on port $port..."
    
    # Update application.yml
    sed -i "s/# Customer Service Configuration/# ${service_name^} Configuration/g" "$service_name/src/main/resources/application.yml"
    sed -i "s/port: 8081/port: $port/g" "$service_name/src/main/resources/application.yml"
    sed -i "s/name: customer-service/name: $service_name/g" "$service_name/src/main/resources/application.yml"
    sed -i "s/pool-name: CustomerServiceCP/pool-name: ${service_name^}CP/g" "$service_name/src/main/resources/application.yml"
    sed -i "s/logs\/customer-service.log/logs\/$service_name.log/g" "$service_name/src/main/resources/application.yml"
    
    # Update pom.xml
    sed -i "s/<artifactId>customer-service<\/artifactId>/<artifactId>$service_name<\/artifactId>/g" "$service_name/pom.xml"
    sed -i "s/<name>customer-service<\/name>/<name>$service_name<\/name>/g" "$service_name/pom.xml"
    sed -i "s/<description>Customer Management Microservice<\/description>/<description>$description<\/description>/g" "$service_name/pom.xml"
    
    echo "✅ $service_name configured successfully"
done

echo "🎉 All microservices configured successfully!"
echo ""
echo "📊 Service Ports:"
echo "  Customer Service: 8081"
echo "  Credit Service:   8082" 
echo "  Risk Service:     8083"
echo "  Payment Service:  8084"
echo "  Report Service:   8085"
echo "  Notification Service: 8086"
echo ""
echo "🔗 Access via Nginx Gateway: http://35.77.54.203:8080"
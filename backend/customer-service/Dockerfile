# Customer Service Dockerfile
FROM docker.io/library/openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要工具用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建应用用户（安全最佳实践）
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 复制Maven构建的jar文件
COPY target/customer-service-*.jar app.jar

# 设置文件权限
RUN chown appuser:appuser app.jar

# 切换到应用用户
USER appuser

# 暴露端口（容器内部端口8080，映射到外部8081）
EXPOSE 8080

# 健康检查配置
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM优化参数
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
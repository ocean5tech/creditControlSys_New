package com.creditcontrol.customer.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;

/**
 * 统一日志切面
 * 记录API调用、数据库调用、业务操作的详细日志
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * API调用日志 - 拦截所有Controller方法
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logApiCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        // 获取HTTP请求信息
        String httpMethod = "";
        String requestUri = "";
        String clientIP = "";
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                httpMethod = request.getMethod();
                requestUri = request.getRequestURI();
                clientIP = getClientIP(request);
            }
        } catch (Exception e) {
            log.debug("Could not get HTTP request info: {}", e.getMessage());
        }

        // 请求开始日志
        log.info("API_CALL_START: [{}] {}.{} - {} {} - IP: {} - Args: {}", 
                requestId, className, methodName, httpMethod, requestUri, clientIP, 
                sanitizeArgs(args));

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 请求成功日志
            log.info("API_CALL_SUCCESS: [{}] {}.{} - Duration: {}ms - Response: {}", 
                    requestId, className, methodName, duration, sanitizeResult(result));

            // 性能警告
            if (duration > 2000) {
                log.warn("API_PERFORMANCE_WARNING: [{}] {}.{} - Slow response: {}ms", 
                        requestId, className, methodName, duration);
            }

            return result;
        } catch (Throwable e) {
            exception = e;
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 请求失败日志
            log.error("API_CALL_ERROR: [{}] {}.{} - Duration: {}ms - Error: {} - Message: {}", 
                    requestId, className, methodName, duration, e.getClass().getSimpleName(), 
                    e.getMessage(), e);

            throw e;
        }
    }

    /**
     * 数据库调用日志 - 拦截所有Repository方法
     */
    @Around("execution(* com.creditcontrol.customer.repository..*(..))")
    public Object logDatabaseCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        // 数据库调用开始日志
        log.debug("DB_CALL_START: [{}] {}.{} - Args: {}", 
                requestId, className, methodName, sanitizeArgs(args));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 数据库调用成功日志
            log.debug("DB_CALL_SUCCESS: [{}] {}.{} - Duration: {}ms - Records: {}", 
                    requestId, className, methodName, duration, getRecordCount(result));

            // 慢查询警告
            if (duration > 1000) {
                log.warn("DB_SLOW_QUERY: [{}] {}.{} - Slow query: {}ms", 
                        requestId, className, methodName, duration);
            }

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 数据库调用失败日志
            log.error("DB_CALL_ERROR: [{}] {}.{} - Duration: {}ms - Error: {} - Message: {}", 
                    requestId, className, methodName, duration, e.getClass().getSimpleName(), 
                    e.getMessage(), e);

            throw e;
        }
    }

    /**
     * 业务服务日志 - 拦截所有Service方法
     */
    @Around("execution(* com.creditcontrol.customer.service..*(..))")
    public Object logServiceCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        // 业务服务调用开始日志
        log.info("SERVICE_CALL_START: [{}] {}.{} - Args: {}", 
                requestId, className, methodName, sanitizeArgs(args));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 业务服务调用成功日志
            log.info("SERVICE_CALL_SUCCESS: [{}] {}.{} - Duration: {}ms", 
                    requestId, className, methodName, duration);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 业务服务调用失败日志
            log.error("SERVICE_CALL_ERROR: [{}] {}.{} - Duration: {}ms - Error: {} - Message: {}", 
                    requestId, className, methodName, duration, e.getClass().getSimpleName(), 
                    e.getMessage(), e);

            throw e;
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 清理敏感参数信息
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // 简化参数显示，避免过长的日志
            Object[] sanitized = Arrays.stream(args)
                    .map(arg -> {
                        if (arg == null) return "null";
                        if (arg instanceof String) {
                            String str = (String) arg;
                            return str.length() > 100 ? str.substring(0, 100) + "..." : str;
                        }
                        return arg.getClass().getSimpleName() + "@" + Integer.toHexString(arg.hashCode());
                    })
                    .toArray();
            
            return Arrays.toString(sanitized);
        } catch (Exception e) {
            return "[Error serializing args: " + e.getMessage() + "]";
        }
    }

    /**
     * 清理返回结果信息
     */
    private String sanitizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            // 避免循环引用和过长的日志
            String className = result.getClass().getSimpleName();
            if (result instanceof String) {
                String str = (String) result;
                return str.length() > 200 ? str.substring(0, 200) + "..." : str;
            } else if (result instanceof Number || result instanceof Boolean) {
                return result.toString();
            } else {
                return className + "@" + Integer.toHexString(result.hashCode());
            }
        } catch (Exception e) {
            return "[Error serializing result: " + e.getMessage() + "]";
        }
    }

    /**
     * 获取返回记录数量
     */
    private String getRecordCount(Object result) {
        if (result == null) {
            return "0";
        }
        
        try {
            if (result instanceof java.util.Collection) {
                return String.valueOf(((java.util.Collection<?>) result).size());
            } else if (result instanceof java.util.Optional) {
                return ((java.util.Optional<?>) result).isPresent() ? "1" : "0";
            } else if (result.getClass().isArray()) {
                return String.valueOf(java.lang.reflect.Array.getLength(result));
            } else {
                return "1";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
}
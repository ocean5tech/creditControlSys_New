package com.creditcontrol.customer.exception;

/**
 * 客户未找到异常
 */
public class CustomerNotFoundException extends RuntimeException {
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
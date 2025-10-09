package com.creditcontrol.customer.exception;

/**
 * 客户代码重复异常
 */
public class DuplicateCustomerException extends RuntimeException {
    
    public DuplicateCustomerException(String message) {
        super(message);
    }
    
    public DuplicateCustomerException(String message, Throwable cause) {
        super(message, cause);
    }
}
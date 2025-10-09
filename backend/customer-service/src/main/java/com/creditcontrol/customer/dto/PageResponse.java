package com.creditcontrol.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应DTO
 * 用于包装分页查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private List<T> content;
    
    private long totalElements;
    
    private int totalPages;
    
    private int currentPage;
    
    private int pageSize;
    
    private boolean first;
    
    private boolean last;
    
    private boolean empty;
    
    // 便捷构造方法
    public static <T> PageResponse<T> of(List<T> content, long totalElements, 
                                         int currentPage, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        
        return PageResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .first(currentPage == 0)
                .last(currentPage == totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }
}
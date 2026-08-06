package com.company.banking.customer.application;

import java.util.List;

/**
 * Paginated response for customer list endpoint.
 */
public class CustomerListResponse {

    private List<CustomerResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public CustomerListResponse() {
    }

    public CustomerListResponse(
            List<CustomerResponse> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize
    ) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<CustomerResponse> getContent() {
        return content;
    }

    public void setContent(List<CustomerResponse> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

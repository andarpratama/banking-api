package com.company.banking.account.application;

import java.util.List;

/**
 * List response for {@code GET /customers/{customerId}/accounts}.
 */
public class AccountListResponse {

    private List<AccountResponse> content;
    private long totalElements;

    public AccountListResponse() {
    }

    public AccountListResponse(List<AccountResponse> content, long totalElements) {
        this.content = content;
        this.totalElements = totalElements;
    }

    public List<AccountResponse> getContent() {
        return content;
    }

    public void setContent(List<AccountResponse> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}

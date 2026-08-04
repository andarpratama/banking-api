package com.company.banking.common.pagination;

import java.util.List;

/**
 * Shared paginated response shape aligned with OpenAPI list endpoints:
 * {@code content}, {@code totalElements}, {@code totalPages}, {@code currentPage}, {@code pageSize}.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, PageQuery query) {
        return of(content, totalElements, query.page(), query.size());
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        int totalPages = (int) Math.ceil((double) totalElements / (double) size);
        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }
}

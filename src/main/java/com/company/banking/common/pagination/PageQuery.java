package com.company.banking.common.pagination;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;

/**
 * Shared pagination / sort request params aligned with OpenAPI
 * ({@code page}, {@code size}, {@code sort}).
 */
public final class PageQuery {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT_PROPERTY = "createdAt";
    public static final String DEFAULT_SORT = "createdAt,desc";

    private final int page;
    private final int size;
    private final SortSpec sort;

    private PageQuery(int page, int size, SortSpec sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public static PageQuery of(Integer page, Integer size, String sort) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        if (resolvedPage < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page must be >= 0");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "size must be between 1 and " + MAX_SIZE
            );
        }
        SortSpec resolvedSort;
        try {
            resolvedSort = SortSpec.parse(
                    sort == null || sort.isBlank() ? DEFAULT_SORT : sort
            );
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid sort parameter");
        }
        return new PageQuery(resolvedPage, resolvedSize, resolvedSort);
    }

    public static PageQuery defaults() {
        return of(null, null, null);
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public SortSpec sort() {
        return sort;
    }

    public long offset() {
        return (long) page * size;
    }
}

package com.company.banking.common.pagination;

/**
 * Sort direction for shared pagination query params ({@code sort=field,asc|desc}).
 */
public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        return SortDirection.valueOf(value.trim().toUpperCase());
    }
}

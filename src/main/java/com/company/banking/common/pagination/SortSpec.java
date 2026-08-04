package com.company.banking.common.pagination;

/**
 * Parsed {@code sort} query value, e.g. {@code createdAt,desc}.
 */
public record SortSpec(String property, SortDirection direction) {

    public SortSpec {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("Sort property is required");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Sort direction is required");
        }
        property = property.trim();
    }

    public static SortSpec parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec(PageQuery.DEFAULT_SORT_PROPERTY, SortDirection.DESC);
        }
        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        SortDirection direction = parts.length > 1
                ? SortDirection.from(parts[1])
                : SortDirection.DESC;
        return new SortSpec(property, direction);
    }
}

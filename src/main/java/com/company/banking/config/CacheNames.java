package com.company.banking.config;

/**
 * Spring Cache region names for customer/account read-through.
 */
public final class CacheNames {

    public static final String CUSTOMERS = "customers";
    public static final String ACCOUNTS = "accounts";

    private CacheNames() {
    }
}

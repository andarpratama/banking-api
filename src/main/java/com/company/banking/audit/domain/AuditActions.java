package com.company.banking.audit.domain;

/**
 * Canonical action names for sensitive operations (auth, account state, money movement).
 */
public final class AuditActions {

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    public static final String CREATE_ACCOUNT = "CREATE_ACCOUNT";
    public static final String FREEZE_ACCOUNT = "FREEZE_ACCOUNT";
    public static final String UNFREEZE_ACCOUNT = "UNFREEZE_ACCOUNT";
    public static final String CLOSE_ACCOUNT = "CLOSE_ACCOUNT";

    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAW = "WITHDRAW";
    public static final String TRANSFER_MONEY = "TRANSFER_MONEY";

    private AuditActions() {
    }
}

package com.company.banking.security;

import com.company.banking.auth.infrastructure.persistence.SpringDataCustomerRepository;
import com.company.banking.auth.infrastructure.persistence.SpringDataUserRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to extract security context information (username, roles, etc.).
 * Used for ownership checks and security-related operations.
 */
@Component
public class SecurityContextHelper {

    private final SpringDataUserRepository userRepository;
    private final SpringDataCustomerRepository customerRepository;

    public SecurityContextHelper(
            SpringDataUserRepository userRepository,
            SpringDataCustomerRepository customerRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Get the currently authenticated username (email) from security context.
     *
     * @return the username/email of the authenticated user, or null if not authenticated
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param role the role to check (without "ROLE_" prefix, e.g., "ADMIN" or "CUSTOMER")
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String roleWithPrefix = "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Check if the current user is an ADMIN.
     *
     * @return true if user has ADMIN role
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if the current user is a CUSTOMER.
     *
     * @return true if user has CUSTOMER role
     */
    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    /**
     * Check if the current authenticated user owns the given customer ID.
     * Used for ownership-based authorization.
     *
     * @param customerId the customer ID to check ownership for
     * @return true if current user is the owner of this customer, false otherwise
     */
    public boolean isOwner(UUID customerId) {
        String currentEmail = getCurrentUsername();
        if (currentEmail == null) {
            return false;
        }

        return userRepository.findByEmailIgnoreCase(currentEmail)
                .flatMap(userEntity -> customerRepository.findByUserId(userEntity.getId()))
                .map(customerEntity -> customerEntity.getId().equals(customerId))
                .orElse(false);
    }
}

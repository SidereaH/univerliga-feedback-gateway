package com.univerliga.gateway.security;

import java.util.Set;

public record CurrentUser(String username, String personId, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(SecurityRoles.ADMIN);
    }

    public boolean isManager() {
        return hasRole(SecurityRoles.MANAGER);
    }

    public boolean isEmployee() {
        return hasRole(SecurityRoles.EMPLOYEE);
    }
}

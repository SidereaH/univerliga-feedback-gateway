package com.univerliga.gateway.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    public CurrentUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (!(principal instanceof JwtAuthenticationToken auth)) {
            return new CurrentUser("anonymous", "p_anonymous", Set.of());
        }
        Jwt jwt = auth.getToken();
        String username = jwt.getClaimAsString("preferred_username");
        Set<String> roles = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .collect(Collectors.toSet());
        return new CurrentUser(username, mapPersonId(username), roles);
    }

    private String mapPersonId(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return "p_admin";
        }
        if ("manager".equalsIgnoreCase(username)) {
            return "p_manager";
        }
        if ("employee".equalsIgnoreCase(username)) {
            return "p_employee";
        }
        if ("hr".equalsIgnoreCase(username)) {
            return "p_hr";
        }
        return "p_" + username;
    }
}

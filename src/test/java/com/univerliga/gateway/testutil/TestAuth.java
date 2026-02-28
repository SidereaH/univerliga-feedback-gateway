package com.univerliga.gateway.testutil;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.Arrays;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public final class TestAuth {
    private TestAuth() {
    }

    public static JwtRequestPostProcessor jwtFor(String username, String... roles) {
        Collection<GrantedAuthority> authorities =
            Arrays.stream(roles).map(SimpleGrantedAuthority::new).map(a -> (GrantedAuthority) a).toList();
        return SecurityMockMvcRequestPostProcessors.jwt()
            .jwt(jwt -> jwt.claim("preferred_username", username))
            .authorities(authorities);
    }
}

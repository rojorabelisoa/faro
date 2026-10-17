package com.faro.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RoleExtractionTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void mapsRolesClaimToSpringRoles() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("s")
            .claim("roles", List.of("user", "admin")).build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .filteredOn(authority -> authority.startsWith("ROLE_"))
            .containsExactlyInAnyOrder("ROLE_user", "ROLE_admin");
    }

    @Test
    void tokenWithoutRolesHasNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("s").build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .noneMatch(authority -> authority.startsWith("ROLE_"));
    }
}

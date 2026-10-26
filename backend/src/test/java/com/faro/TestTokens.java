package com.faro;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/** Jetons de test : un utilisateur = un "sub" = sa propre organisation. */
public final class TestTokens {

    private TestTokens() {
    }

    public static JwtRequestPostProcessor user(String subject) {
        return jwt()
            .jwt(token -> token.subject(subject).claim("preferred_username", subject))
            .authorities(new SimpleGrantedAuthority("ROLE_user"));
    }
}

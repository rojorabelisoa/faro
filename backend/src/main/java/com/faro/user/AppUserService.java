package com.faro.user;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    /** Crée l'utilisateur à sa première visite, puis met à jour son profil depuis le jeton. */
    @Transactional
    public AppUser syncFromToken(Jwt jwt) {
        AppUser user = repository.findBySubject(jwt.getSubject())
            .orElseGet(() -> new AppUser(jwt.getSubject()));
        user.setEmail(jwt.getClaimAsString("email"));
        user.setDisplayName(displayName(jwt));
        user.setLastLoginAt(Instant.now());
        return repository.save(user);
    }

    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("preferred_username");
    }
}

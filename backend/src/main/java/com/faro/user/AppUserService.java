package com.faro.user;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.organisation.OrganisationService;

@Service
public class AppUserService {

    static final String NOM_ORGANISATION_PAR_DEFAUT = "Mon entreprise";

    private final AppUserRepository repository;
    private final OrganisationService organisationService;

    public AppUserService(AppUserRepository repository, OrganisationService organisationService) {
        this.repository = repository;
        this.organisationService = organisationService;
    }

    /** Crée l'utilisateur à sa première visite, puis met à jour son profil depuis le jeton. */
    @Transactional
    public AppUser syncFromToken(Jwt jwt) {
        AppUser user = findOrCreate(jwt);
        user.setEmail(jwt.getClaimAsString("email"));
        user.setDisplayName(displayName(jwt));
        user.setLastLoginAt(Instant.now());
        return repository.save(user);
    }

    /** Utilisateur courant, avec son organisation (créés si besoin). */
    @Transactional
    public AppUser current(Jwt jwt) {
        return findOrCreate(jwt);
    }

    private AppUser findOrCreate(Jwt jwt) {
        AppUser user = repository.findBySubject(jwt.getSubject()).orElseGet(() -> {
            AppUser created = new AppUser(jwt.getSubject());
            created.setEmail(jwt.getClaimAsString("email"));
            created.setDisplayName(displayName(jwt));
            return created;
        });
        // En attendant les invitations, chaque nouvel utilisateur a sa propre organisation.
        if (user.getOrganisation() == null) {
            user.setOrganisation(organisationService.create(NOM_ORGANISATION_PAR_DEFAUT));
        }
        return repository.save(user);
    }

    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("preferred_username");
    }
}

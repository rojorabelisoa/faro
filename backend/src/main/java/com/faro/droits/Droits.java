package com.faro.droits;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.faro.organisation.RoleEntreprise;
import com.faro.user.AppUser;
import com.faro.user.AppUserService;

/**
 * Droits de l'utilisateur courant selon son rôle dans l'entreprise (matrice dans PROGRESS.md, « Décisions ») :
 * le gérant fait tout, le chargé d'études tout sauf administrer l'entreprise (vérifié par EquipeService),
 * le conducteur de travaux seulement ce qui est ouvert par {@link AccesConducteur} et ne voit pas les coûts ni la marge.
 */
@Component
public class Droits {

    static final String MESSAGE_CONDUCTEUR =
        "Action non autorisée pour un conducteur de travaux : demandez au gérant ou au chargé d'études.";

    private static final String ATTRIBUT_UTILISATEUR = Droits.class.getName() + ".utilisateur";

    private final AppUserService userService;

    public Droits(AppUserService userService) {
        this.userService = userService;
    }

    /** Utilisateur de la requête en cours (chargé une seule fois par requête), null hors requête authentifiée. */
    public AppUser utilisateur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            return null;
        }
        RequestAttributes requete = RequestContextHolder.getRequestAttributes();
        if (requete != null && requete.getAttribute(ATTRIBUT_UTILISATEUR, RequestAttributes.SCOPE_REQUEST) instanceof AppUser user) {
            return user;
        }
        AppUser user = userService.current(jwt.getToken());
        if (requete != null) {
            requete.setAttribute(ATTRIBUT_UTILISATEUR, user, RequestAttributes.SCOPE_REQUEST);
        }
        return user;
    }

    /** Rôle de l'utilisateur courant ; null hors requête (traitements internes, données de démo). */
    public RoleEntreprise role() {
        AppUser user = utilisateur();
        return user == null ? null : user.getRoleEntreprise();
    }

    public boolean estConducteur() {
        return role() == RoleEntreprise.CONDUCTEUR_TRAVAUX;
    }

    /** Coûts (déboursé sec), marges, bilan investisseur et montants du tableau de bord. */
    public boolean voitLesMontantsInternes() {
        return !estConducteur();
    }

    /** Refuse l'action au conducteur de travaux (403). */
    public void refuserConducteur() {
        if (estConducteur()) {
            throw interdit();
        }
    }

    static ResponseStatusException interdit() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, MESSAGE_CONDUCTEUR);
    }
}

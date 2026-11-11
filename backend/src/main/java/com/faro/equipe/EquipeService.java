package com.faro.equipe;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.common.NotFoundException;
import com.faro.equipe.EquipeDtos.EntrepriseResponse;
import com.faro.equipe.EquipeDtos.InvitationInfo;
import com.faro.equipe.EquipeDtos.Membre;
import com.faro.organisation.Organisation;
import com.faro.organisation.RoleEntreprise;
import com.faro.user.AppUser;
import com.faro.user.AppUserRepository;

@Service
@Transactional
public class EquipeService {

    static final Duration VALIDITE_INVITATION = Duration.ofDays(7);
    private static final SecureRandom ALEA = new SecureRandom();

    private final AppUserRepository users;
    private final InvitationRepository invitations;
    private final JdbcTemplate jdbc;

    public EquipeService(AppUserRepository users, InvitationRepository invitations, JdbcTemplate jdbc) {
        this.users = users;
        this.invitations = invitations;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public EntrepriseResponse entreprise(AppUser user) {
        user = attache(user);
        Organisation organisation = user.getOrganisation();
        List<Membre> membres = users.findByOrganisationIdOrderByDisplayNameAsc(organisation.getId()).stream()
            .map(u -> new Membre(u.getId(), u.getDisplayName(), u.getEmail(), u.getRoleEntreprise())).toList();
        return new EntrepriseResponse(organisation.getId(), organisation.getNom(), membres);
    }

    public void renommer(AppUser gerant, String nom) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        gerant.getOrganisation().setNom(nom.trim());
    }

    public void changerRole(AppUser gerant, UUID membreId, RoleEntreprise role) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        AppUser membre = membre(gerant, membreId);
        if (membre.getRoleEntreprise() == RoleEntreprise.GERANT && role != RoleEntreprise.GERANT) {
            verifierAutreGerant(gerant);
        }
        membre.setRoleEntreprise(role);
    }

    /** Le membre retiré retrouvera une entreprise à lui à sa prochaine connexion. */
    public void retirer(AppUser gerant, UUID membreId) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        AppUser membre = membre(gerant, membreId);
        if (membre.getRoleEntreprise() == RoleEntreprise.GERANT) {
            verifierAutreGerant(gerant);
        }
        membre.setOrganisation(null);
    }

    @Transactional(readOnly = true)
    public List<Invitation> invitations(AppUser gerant) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        return invitations.findByOrganisationIdAndUsedAtIsNullOrderByCreatedAtDesc(gerant.getOrganisation().getId());
    }

    public Invitation inviter(AppUser gerant, RoleEntreprise role, String email) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        byte[] octets = new byte[24];
        ALEA.nextBytes(octets);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        return invitations.save(new Invitation(gerant.getOrganisation(), code, role,
            email == null || email.isBlank() ? null : email.trim(), Instant.now().plus(VALIDITE_INVITATION)));
    }

    public void revoquer(AppUser gerant, UUID invitationId) {
        gerant = attache(gerant);
        verifierGerant(gerant);
        invitations.delete(invitations.findByIdAndOrganisationId(invitationId, gerant.getOrganisation().getId())
            .orElseThrow(() -> new NotFoundException("Invitation introuvable")));
    }

    @Transactional(readOnly = true)
    public InvitationInfo info(String code) {
        Invitation invitation = invitation(code);
        return new InvitationInfo(invitation.getOrganisation().getNom(), invitation.getRoleEntreprise(),
            invitation.isUtilisable());
    }

    /**
     * Rattache l'utilisateur à l'entreprise de l'invitation. L'entreprise qu'il avait eue à sa première connexion
     * est supprimée si elle est vide (lui seul, aucune affaire).
     */
    public void accepter(AppUser user, String code) {
        user = attache(user);
        Invitation invitation = invitation(code);
        if (!invitation.isUtilisable()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Cette invitation a expiré ou a déjà été utilisée");
        }
        Organisation ancienne = user.getOrganisation();
        if (ancienne != null && ancienne.getId().equals(invitation.getOrganisation().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous faites déjà partie de cette entreprise");
        }
        if (ancienne != null && user.getRoleEntreprise() == RoleEntreprise.GERANT
                && users.countByOrganisationIdAndRoleEntreprise(ancienne.getId(), RoleEntreprise.GERANT) == 1
                && users.findByOrganisationIdOrderByDisplayNameAsc(ancienne.getId()).size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Vous êtes le seul gérant de votre entreprise actuelle : nommez un autre gérant avant de la quitter");
        }
        user.setOrganisation(invitation.getOrganisation());
        user.setRoleEntreprise(invitation.getRoleEntreprise());
        invitation.utiliser(user.getId());
        users.flush();
        if (ancienne != null) {
            supprimerSiVide(ancienne.getId());
        }
    }

    private void supprimerSiVide(UUID organisationId) {
        Integer membres = jdbc.queryForObject("SELECT count(*) FROM app_user WHERE organisation_id = ?", Integer.class,
            organisationId);
        Integer affaires = jdbc.queryForObject("SELECT count(*) FROM affaire WHERE organisation_id = ?", Integer.class,
            organisationId);
        if (membres == 0 && affaires == 0) {
            for (String table : List.of("ouvrage", "ratio", "lot", "invitation")) {
                jdbc.update("DELETE FROM " + table + " WHERE organisation_id = ?", organisationId);
            }
            jdbc.update("DELETE FROM organisation WHERE id = ?", organisationId);
        }
    }

    /** L'utilisateur fourni par le contrôleur vient d'une autre transaction : on le recharge. */
    private AppUser attache(AppUser user) {
        return users.findById(user.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    private Invitation invitation(String code) {
        return invitations.findByCode(code).orElseThrow(() -> new NotFoundException("Invitation introuvable"));
    }

    private AppUser membre(AppUser gerant, UUID membreId) {
        return users.findById(membreId)
            .filter(u -> u.getOrganisation() != null && u.getOrganisation().getId().equals(gerant.getOrganisation().getId()))
            .orElseThrow(() -> new NotFoundException("Membre introuvable"));
    }

    private void verifierAutreGerant(AppUser gerant) {
        if (users.countByOrganisationIdAndRoleEntreprise(gerant.getOrganisation().getId(), RoleEntreprise.GERANT) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'entreprise doit garder au moins un gérant");
        }
    }

    static void verifierGerant(AppUser user) {
        if (user.getRoleEntreprise() != RoleEntreprise.GERANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé au gérant de l'entreprise");
        }
    }
}

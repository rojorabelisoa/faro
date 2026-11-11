package com.faro.equipe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.faro.organisation.RoleEntreprise;

/** Requêtes et réponses de la gestion d'équipe. */
public final class EquipeDtos {

    private EquipeDtos() {
    }

    public record EntrepriseResponse(UUID id, String nom, List<Membre> membres, Offre offre) {
    }

    /** maxUtilisateurs null : illimité. essaiFin : seulement pendant l'essai. */
    public record Offre(com.faro.organisation.Plan plan, String libelle, Integer maxUtilisateurs, Instant essaiFin) {

        public static Offre de(com.faro.organisation.Organisation organisation) {
            com.faro.organisation.Plan plan = organisation.getPlan();
            return new Offre(plan, plan.getLibelle(),
                plan.getMaxUtilisateurs() == Integer.MAX_VALUE ? null : plan.getMaxUtilisateurs(),
                plan == com.faro.organisation.Plan.ESSAI ? organisation.getEssaiFin() : null);
        }
    }

    public record Membre(UUID id, String nom, String email, RoleEntreprise role) {
    }

    public record RenommerRequest(@NotBlank @Size(max = 255) String nom) {
    }

    public record RoleRequest(@NotNull RoleEntreprise role) {
    }

    public record InvitationRequest(@NotNull RoleEntreprise role, @Email @Size(max = 320) String email) {
    }

    public record InvitationResponse(UUID id, String code, RoleEntreprise role, String email, Instant expiresAt) {

        static InvitationResponse from(Invitation invitation) {
            return new InvitationResponse(invitation.getId(), invitation.getCode(), invitation.getRoleEntreprise(),
                invitation.getEmail(), invitation.getExpiresAt());
        }
    }

    /** Ce que voit la personne invitée avant d'accepter. */
    public record InvitationInfo(String entreprise, RoleEntreprise role, boolean utilisable) {
    }
}

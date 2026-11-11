package com.faro.organisation;

/**
 * Offres du business plan (Starter solo, Pro équipe, Business multi-agences).
 * Limites INDICATIVES à valider ; les prix ne sont pas gérés ici (pas encore de paiement en ligne).
 */
public enum Plan {
    ESSAI("Essai gratuit", 3),
    STARTER("Starter", 1),
    PRO("Pro", 10),
    BUSINESS("Business", Integer.MAX_VALUE);

    private final String libelle;
    private final int maxUtilisateurs;

    Plan(String libelle, int maxUtilisateurs) {
        this.libelle = libelle;
        this.maxUtilisateurs = maxUtilisateurs;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getMaxUtilisateurs() {
        return maxUtilisateurs;
    }
}

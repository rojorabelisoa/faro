package com.faro.avenant;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.faro.affaire.Affaire;

/** Travaux supplémentaires (ou en moins) demandés pendant le chantier, à faire valider par le client. */
@Entity
@Table(name = "avenant")
public class Avenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Column(nullable = false)
    private String objet;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAvenant statut = StatutAvenant.BROUILLON;

    /** Contenu exact envoyé au client (JSON), base de la décision. */
    @Column(name = "contenu_envoye")
    private String contenuEnvoye;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "envoye_at")
    private Instant envoyeAt;

    @Column(name = "decide_at")
    private Instant decideAt;

    @Column(name = "decide_par")
    private String decidePar;

    @Column(name = "decision_commentaire")
    private String decisionCommentaire;

    protected Avenant() {
    }

    public Avenant(Affaire affaire, int numero, String objet, String motif) {
        this.affaire = affaire;
        this.numero = numero;
        this.objet = objet;
        this.motif = motif;
        this.createdAt = Instant.now();
    }

    void envoyer(String contenu) {
        this.statut = StatutAvenant.ENVOYE;
        this.contenuEnvoye = contenu;
        this.envoyeAt = Instant.now();
    }

    void decider(StatutAvenant decision, String par, String commentaire) {
        this.statut = decision;
        this.decidePar = par;
        this.decisionCommentaire = commentaire;
        this.decideAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Affaire getAffaire() {
        return affaire;
    }

    public int getNumero() {
        return numero;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public StatutAvenant getStatut() {
        return statut;
    }

    public String getContenuEnvoye() {
        return contenuEnvoye;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEnvoyeAt() {
        return envoyeAt;
    }

    public Instant getDecideAt() {
        return decideAt;
    }

    public String getDecidePar() {
        return decidePar;
    }

    public String getDecisionCommentaire() {
        return decisionCommentaire;
    }
}

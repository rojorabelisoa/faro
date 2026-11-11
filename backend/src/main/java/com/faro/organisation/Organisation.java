package com.faro.organisation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Entreprise cliente de faro : toutes les données métier lui sont rattachées. */
@Entity
@Table(name = "organisation")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.ESSAI;

    @Column(name = "essai_fin")
    private Instant essaiFin;

    protected Organisation() {
    }

    public Organisation(String nom) {
        this.nom = nom;
        this.createdAt = Instant.now();
        this.essaiFin = this.createdAt.plus(DUREE_ESSAI);
    }

    public static final java.time.Duration DUREE_ESSAI = java.time.Duration.ofDays(30);

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public Instant getEssaiFin() {
        return essaiFin;
    }
}

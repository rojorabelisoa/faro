package com.faro.affaire;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pièce (zone) d'une affaire : la façon dont le client voit son projet. */
@Entity
@Table(name = "piece")
public class Piece {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @Column(nullable = false)
    private String nom;

    @Column(name = "surface_m2")
    private BigDecimal surfaceM2;

    @Column(name = "perimetre_ml")
    private BigDecimal perimetreMl;

    @Column(name = "hauteur_m")
    private BigDecimal hauteurM;

    @Column(nullable = false)
    private int ordre;

    protected Piece() {
    }

    public Piece(Affaire affaire, String nom, BigDecimal surfaceM2, int ordre) {
        this.affaire = affaire;
        this.nom = nom;
        this.surfaceM2 = surfaceM2;
        this.ordre = ordre;
    }

    public UUID getId() {
        return id;
    }

    public Affaire getAffaire() {
        return affaire;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(BigDecimal surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public BigDecimal getPerimetreMl() {
        return perimetreMl;
    }

    public void setPerimetreMl(BigDecimal perimetreMl) {
        this.perimetreMl = perimetreMl;
    }

    public BigDecimal getHauteurM() {
        return hauteurM;
    }

    public void setHauteurM(BigDecimal hauteurM) {
        this.hauteurM = hauteurM;
    }

    /** Surface des murs (périmètre × hauteur, ouvertures non déduites), ou null si l'un manque. */
    public BigDecimal getSurfaceMursM2() {
        return perimetreMl != null && hauteurM != null
            ? perimetreMl.multiply(hauteurM).setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}

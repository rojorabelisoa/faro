package com.faro.affaire;

import java.math.BigDecimal;
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

import com.faro.devis.Gamme;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type_piece")
    private TypePiece typePiece;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_travaux")
    private NiveauTravaux niveauTravaux;

    @Enumerated(EnumType.STRING)
    @Column(name = "gamme_choisie", nullable = false)
    private Gamme gammeChoisie = Gamme.STANDARD;

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

    public TypePiece getTypePiece() {
        return typePiece;
    }

    public void setTypePiece(TypePiece typePiece) {
        this.typePiece = typePiece;
    }

    public NiveauTravaux getNiveauTravaux() {
        return niveauTravaux;
    }

    public void setNiveauTravaux(NiveauTravaux niveauTravaux) {
        this.niveauTravaux = niveauTravaux;
    }

    public Gamme getGammeChoisie() {
        return gammeChoisie;
    }

    public void setGammeChoisie(Gamme gammeChoisie) {
        this.gammeChoisie = gammeChoisie;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}

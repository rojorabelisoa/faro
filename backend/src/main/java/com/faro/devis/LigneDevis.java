package com.faro.devis;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.faro.affaire.Affaire;
import com.faro.affaire.Piece;
import com.faro.lot.Lot;

/**
 * Un ouvrage chiffré du devis. Il est saisi une seule fois et rattaché à la fois à un lot
 * et (facultativement) à une pièce : on peut ainsi présenter le devis par pièce ou par lot.
 */
@Entity
@Table(name = "ligne_devis")
public class LigneDevis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    /** Null : ligne générale, non rattachée à une pièce (installation de chantier, évacuation…). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id")
    private Piece piece;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @Column(nullable = false)
    private String designation;

    private String description;

    @Column(nullable = false)
    private String unite;

    @Column(nullable = false)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire_ht", nullable = false)
    private BigDecimal prixUnitaireHt;

    @Column(name = "taux_tva", nullable = false)
    private BigDecimal tauxTva;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LigneDevis() {
    }

    public LigneDevis(Affaire affaire, int ordre) {
        this.affaire = affaire;
        this.ordre = ordre;
        this.createdAt = Instant.now();
    }

    /** Montant HT de la ligne, arrondi au centime. */
    public BigDecimal getMontantHt() {
        return Montants.montantHt(quantite, prixUnitaireHt);
    }

    public UUID getId() {
        return id;
    }

    public Affaire getAffaire() {
        return affaire;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaireHt() {
        return prixUnitaireHt;
    }

    public void setPrixUnitaireHt(BigDecimal prixUnitaireHt) {
        this.prixUnitaireHt = prixUnitaireHt;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.faro.marge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.faro.lot.Lot;

/** Dépense réelle du chantier (facture fournisseur, sous-traitant, heures…), imputée à un lot. */
@Entity
@Table(name = "depense")
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NatureDepense nature;

    @Column(nullable = false)
    private String libelle;

    private String fournisseur;

    @Column(name = "montant_ht", nullable = false)
    private BigDecimal montantHt;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Depense() {
    }

    public Depense(Affaire affaire) {
        this.affaire = affaire;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public NatureDepense getNature() {
        return nature;
    }

    public void setNature(NatureDepense nature) {
        this.nature = nature;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public BigDecimal getMontantHt() {
        return montantHt;
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
    }

    public LocalDate getDateDepense() {
        return dateDepense;
    }

    public void setDateDepense(LocalDate dateDepense) {
        this.dateDepense = dateDepense;
    }
}

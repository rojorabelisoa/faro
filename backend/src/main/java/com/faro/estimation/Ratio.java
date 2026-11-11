package com.faro.estimation;

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

import com.faro.affaire.NiveauTravaux;
import com.faro.affaire.TypePiece;
import com.faro.organisation.Organisation;

/** Fourchette de prix HT au m² pour un type de pièce et un niveau de travaux. */
@Entity
@Table(name = "ratio")
public class Ratio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false, updatable = false)
    private Organisation organisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_piece", nullable = false, updatable = false)
    private TypePiece typePiece;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_travaux", nullable = false, updatable = false)
    private NiveauTravaux niveauTravaux;

    @Column(name = "prix_min_m2", nullable = false)
    private BigDecimal prixMinM2;

    @Column(name = "prix_max_m2", nullable = false)
    private BigDecimal prixMaxM2;

    protected Ratio() {
    }

    public Ratio(Organisation organisation, TypePiece typePiece, NiveauTravaux niveauTravaux, BigDecimal prixMinM2,
            BigDecimal prixMaxM2) {
        this.organisation = organisation;
        this.typePiece = typePiece;
        this.niveauTravaux = niveauTravaux;
        this.prixMinM2 = prixMinM2;
        this.prixMaxM2 = prixMaxM2;
    }

    public UUID getId() {
        return id;
    }

    public TypePiece getTypePiece() {
        return typePiece;
    }

    public NiveauTravaux getNiveauTravaux() {
        return niveauTravaux;
    }

    public BigDecimal getPrixMinM2() {
        return prixMinM2;
    }

    public void setPrixMinM2(BigDecimal prixMinM2) {
        this.prixMinM2 = prixMinM2;
    }

    public BigDecimal getPrixMaxM2() {
        return prixMaxM2;
    }

    public void setPrixMaxM2(BigDecimal prixMaxM2) {
        this.prixMaxM2 = prixMaxM2;
    }
}

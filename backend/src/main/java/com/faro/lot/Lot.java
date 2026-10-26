package com.faro.lot;

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

import com.faro.organisation.Organisation;

/** Lot / corps d'état (plomberie, électricité…) : la façon dont le chantier s'organise. */
@Entity
@Table(name = "lot")
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false, updatable = false)
    private Organisation organisation;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private int ordre;

    protected Lot() {
    }

    public Lot(Organisation organisation, String code, String nom, int ordre) {
        this.organisation = organisation;
        this.code = code;
        this.nom = nom;
        this.ordre = ordre;
    }

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}

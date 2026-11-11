package com.faro.version;

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

/** Copie figée du devis à un instant donné (JSON de DevisResponse + en-tête) : jamais modifiée. */
@Entity
@Table(name = "devis_version")
public class DevisVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Column(nullable = false, updatable = false)
    private String libelle;

    @Column(nullable = false, updatable = false)
    private String contenu;

    @Column(name = "total_ht", nullable = false, updatable = false)
    private BigDecimal totalHt;

    @Column(name = "total_ttc", nullable = false, updatable = false)
    private BigDecimal totalTtc;

    @Column(updatable = false)
    private String auteur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DevisVersion() {
    }

    public DevisVersion(Affaire affaire, int numero, String libelle, String contenu, BigDecimal totalHt,
            BigDecimal totalTtc, String auteur) {
        this.affaire = affaire;
        this.numero = numero;
        this.libelle = libelle;
        this.contenu = contenu;
        this.totalHt = totalHt;
        this.totalTtc = totalTtc;
        this.auteur = auteur;
        this.createdAt = Instant.now();
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

    public String getLibelle() {
        return libelle;
    }

    public String getContenu() {
        return contenu;
    }

    public BigDecimal getTotalHt() {
        return totalHt;
    }

    public BigDecimal getTotalTtc() {
        return totalTtc;
    }

    public String getAuteur() {
        return auteur;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

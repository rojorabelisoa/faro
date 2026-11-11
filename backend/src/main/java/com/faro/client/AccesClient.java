package com.faro.client;

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

/** Lien secret donnant au client l'accès à son affaire, sans compte. Révocable. */
@Entity
@Table(name = "acces_client")
public class AccesClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoque_at")
    private Instant revoqueAt;

    @Column(name = "derniere_visite")
    private Instant derniereVisite;

    protected AccesClient() {
    }

    public AccesClient(Affaire affaire, String code) {
        this.affaire = affaire;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Affaire getAffaire() {
        return affaire;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevoqueAt() {
        return revoqueAt;
    }

    void revoquer() {
        this.revoqueAt = Instant.now();
    }

    public Instant getDerniereVisite() {
        return derniereVisite;
    }

    void visite() {
        this.derniereVisite = Instant.now();
    }
}

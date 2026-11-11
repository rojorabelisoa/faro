package com.faro.visite;

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

/** Note prise pendant la visite, pour l'affaire ou pour une pièce. */
@Entity
@Table(name = "observation")
public class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id")
    private Piece piece;

    @Column(nullable = false)
    private String texte;

    private String auteur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Observation() {
    }

    public Observation(Affaire affaire, String auteur) {
        this.affaire = affaire;
        this.auteur = auteur;
        this.createdAt = Instant.now();
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

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public String getAuteur() {
        return auteur;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

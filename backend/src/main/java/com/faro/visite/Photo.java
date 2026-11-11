package com.faro.visite;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Basic;
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

/** Photo de visite, stockée en base (sauvegardée avec le reste des données). */
@Entity
@Table(name = "photo")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affaire_id", nullable = false, updatable = false)
    private Affaire affaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id")
    private Piece piece;

    private String legende;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private int taille;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] donnees;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Photo() {
    }

    public Photo(Affaire affaire, Piece piece, String legende, String contentType, byte[] donnees) {
        this.affaire = affaire;
        this.piece = piece;
        this.legende = legende;
        this.contentType = contentType;
        this.donnees = donnees;
        this.taille = donnees.length;
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

    public String getLegende() {
        return legende;
    }

    public String getContentType() {
        return contentType;
    }

    public int getTaille() {
        return taille;
    }

    public byte[] getDonnees() {
        return donnees;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

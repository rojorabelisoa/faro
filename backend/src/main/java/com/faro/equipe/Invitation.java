package com.faro.equipe;

import java.time.Instant;
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

import com.faro.organisation.Organisation;
import com.faro.organisation.RoleEntreprise;

/** Lien d'invitation à rejoindre une entreprise, à usage unique et limité dans le temps. */
@Entity
@Table(name = "invitation")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false, updatable = false)
    private Organisation organisation;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_entreprise", nullable = false, updatable = false)
    private RoleEntreprise roleEntreprise;

    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by")
    private UUID usedBy;

    protected Invitation() {
    }

    public Invitation(Organisation organisation, String code, RoleEntreprise role, String email, Instant expiresAt) {
        this.organisation = organisation;
        this.code = code;
        this.roleEntreprise = role;
        this.email = email;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isUtilisable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }

    void utiliser(UUID userId) {
        this.usedAt = Instant.now();
        this.usedBy = userId;
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

    public RoleEntreprise getRoleEntreprise() {
        return roleEntreprise;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}

package com.faro.investisseur;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Hypothèses d'une opération d'achat-revente ; les travaux viennent du devis (montant du marché TTC). */
@Entity
@Table(name = "bilan_operation")
public class BilanOperation {

    @Id
    @Column(name = "affaire_id")
    private UUID affaireId;

    @Column(name = "prix_achat", nullable = false)
    private BigDecimal prixAchat;

    @Column(name = "frais_acquisition", nullable = false)
    private BigDecimal fraisAcquisition;

    @Column(name = "autres_frais", nullable = false)
    private BigDecimal autresFrais;

    @Column(name = "prix_revente", nullable = false)
    private BigDecimal prixRevente;

    @Column(name = "duree_mois", nullable = false)
    private int dureeMois;

    protected BilanOperation() {
    }

    public BilanOperation(UUID affaireId) {
        this.affaireId = affaireId;
    }

    public UUID getAffaireId() {
        return affaireId;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public BigDecimal getFraisAcquisition() {
        return fraisAcquisition;
    }

    public void setFraisAcquisition(BigDecimal fraisAcquisition) {
        this.fraisAcquisition = fraisAcquisition;
    }

    public BigDecimal getAutresFrais() {
        return autresFrais;
    }

    public void setAutresFrais(BigDecimal autresFrais) {
        this.autresFrais = autresFrais;
    }

    public BigDecimal getPrixRevente() {
        return prixRevente;
    }

    public void setPrixRevente(BigDecimal prixRevente) {
        this.prixRevente = prixRevente;
    }

    public int getDureeMois() {
        return dureeMois;
    }

    public void setDureeMois(int dureeMois) {
        this.dureeMois = dureeMois;
    }
}

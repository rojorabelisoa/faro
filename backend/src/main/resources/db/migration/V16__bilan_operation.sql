-- Module investisseur / marchand de biens : bilan d'une opération (achat, travaux, revente).
CREATE TABLE bilan_operation (
    affaire_id        UUID PRIMARY KEY REFERENCES affaire (id) ON DELETE CASCADE,
    prix_achat        NUMERIC(14, 2) NOT NULL,
    frais_acquisition NUMERIC(14, 2) NOT NULL,
    autres_frais      NUMERIC(14, 2) NOT NULL,
    prix_revente      NUMERIC(14, 2) NOT NULL,
    duree_mois        INTEGER        NOT NULL
);

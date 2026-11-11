-- Marge : déboursé sec (coût de revient unitaire HT) prévu sur les lignes et les ouvrages,
-- et dépenses réelles du chantier par lot.
ALTER TABLE ligne_devis ADD COLUMN debourse_unitaire_ht NUMERIC(12, 2);
ALTER TABLE ouvrage ADD COLUMN debourse_unitaire_ht NUMERIC(12, 2);

CREATE TABLE depense (
    id          UUID PRIMARY KEY,
    affaire_id  UUID           NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    lot_id      UUID           NOT NULL REFERENCES lot (id),
    nature      VARCHAR(30)    NOT NULL,
    libelle     VARCHAR(500)   NOT NULL,
    fournisseur VARCHAR(255),
    montant_ht  NUMERIC(12, 2) NOT NULL,
    date_depense DATE          NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX depense_affaire_idx ON depense (affaire_id);

-- Ligne de devis : un ouvrage chiffré, rattaché à un lot et (facultativement) à une pièce.
CREATE TABLE ligne_devis (
    id               UUID PRIMARY KEY,
    affaire_id       UUID           NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    piece_id         UUID REFERENCES piece (id),
    lot_id           UUID           NOT NULL REFERENCES lot (id),
    designation      VARCHAR(500)   NOT NULL,
    description      TEXT,
    unite            VARCHAR(20)    NOT NULL,
    quantite         NUMERIC(12, 3) NOT NULL,
    prix_unitaire_ht NUMERIC(12, 2) NOT NULL,
    taux_tva         NUMERIC(5, 2)  NOT NULL,
    ordre            INTEGER        NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ligne_devis_affaire_idx ON ligne_devis (affaire_id);
CREATE INDEX ligne_devis_piece_idx ON ligne_devis (piece_id);
CREATE INDEX ligne_devis_lot_idx ON ligne_devis (lot_id);

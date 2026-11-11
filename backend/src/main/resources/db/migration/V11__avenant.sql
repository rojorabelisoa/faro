-- Avenants (travaux supplémentaires ou en moins) : leurs lignes sont des lignes de devis rattachées à l'avenant.
CREATE TABLE avenant (
    id                   UUID PRIMARY KEY,
    affaire_id           UUID         NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    numero               INTEGER      NOT NULL,
    objet                VARCHAR(255) NOT NULL,
    motif                TEXT,
    statut               VARCHAR(20)  NOT NULL,
    contenu_envoye       TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    envoye_at            TIMESTAMP WITH TIME ZONE,
    decide_at            TIMESTAMP WITH TIME ZONE,
    decide_par           VARCHAR(255),
    decision_commentaire TEXT,
    UNIQUE (affaire_id, numero)
);

ALTER TABLE ligne_devis ADD COLUMN avenant_id UUID REFERENCES avenant (id) ON DELETE CASCADE;
CREATE INDEX ligne_devis_avenant_idx ON ligne_devis (avenant_id);

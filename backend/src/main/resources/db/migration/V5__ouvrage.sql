-- Bibliothèque d'ouvrages de l'organisation (prix de référence).
CREATE TABLE ouvrage (
    id               UUID PRIMARY KEY,
    organisation_id  UUID           NOT NULL REFERENCES organisation (id),
    lot_id           UUID           NOT NULL REFERENCES lot (id),
    designation      VARCHAR(500)   NOT NULL,
    description      TEXT,
    unite            VARCHAR(20)    NOT NULL,
    prix_unitaire_ht NUMERIC(12, 2) NOT NULL,
    taux_tva         NUMERIC(5, 2)  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ouvrage_organisation_idx ON ouvrage (organisation_id);

-- Ouvrage d'origine d'une ligne (les valeurs sont copiées : modifier la bibliothèque ne change pas les devis).
-- Servira à comparer prix de référence, prix devisés et coûts réels.
ALTER TABLE ligne_devis ADD COLUMN ouvrage_id UUID REFERENCES ouvrage (id) ON DELETE SET NULL;

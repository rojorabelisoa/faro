-- Versions figées du devis (envoyées au client, dossier travaux…) : copie complète, jamais modifiée.
CREATE TABLE devis_version (
    id         UUID PRIMARY KEY,
    affaire_id UUID           NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    numero     INTEGER        NOT NULL,
    libelle    VARCHAR(255)   NOT NULL,
    contenu    TEXT           NOT NULL,
    total_ht   NUMERIC(14, 2) NOT NULL,
    total_ttc  NUMERIC(14, 2) NOT NULL,
    auteur     VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (affaire_id, numero)
);

-- Version de référence du chantier (dossier travaux transmis au conducteur).
ALTER TABLE affaire ADD COLUMN version_chantier_id UUID REFERENCES devis_version (id) ON DELETE SET NULL;

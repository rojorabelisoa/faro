-- Compte-rendu de visite : observations et photos, pour l'affaire ou pour une pièce.
CREATE TABLE observation (
    id         UUID PRIMARY KEY,
    affaire_id UUID NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    piece_id   UUID REFERENCES piece (id) ON DELETE SET NULL,
    texte      TEXT NOT NULL,
    auteur     VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX observation_affaire_idx ON observation (affaire_id);

CREATE TABLE photo (
    id           UUID PRIMARY KEY,
    affaire_id   UUID         NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    piece_id     UUID REFERENCES piece (id) ON DELETE SET NULL,
    legende      VARCHAR(500),
    content_type VARCHAR(100) NOT NULL,
    taille       INTEGER      NOT NULL,
    donnees      BYTEA        NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX photo_affaire_idx ON photo (affaire_id);

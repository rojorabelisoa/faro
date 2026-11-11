-- Espace client : lien secret (sans compte) donnant accès au devis et aux avenants d'une affaire.
CREATE TABLE acces_client (
    id               UUID PRIMARY KEY,
    affaire_id       UUID        NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    code             VARCHAR(64) NOT NULL UNIQUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    revoque_at       TIMESTAMP WITH TIME ZONE,
    derniere_visite  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX acces_client_affaire_idx ON acces_client (affaire_id);

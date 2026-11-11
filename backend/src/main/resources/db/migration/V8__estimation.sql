-- Estimation rapide : type de pièce et niveau de travaux, ratios €/m² HT par organisation.
ALTER TABLE piece ADD COLUMN type_piece VARCHAR(30);
ALTER TABLE piece ADD COLUMN niveau_travaux VARCHAR(30);

CREATE TABLE ratio (
    id              UUID PRIMARY KEY,
    organisation_id UUID          NOT NULL REFERENCES organisation (id),
    type_piece      VARCHAR(30)   NOT NULL,
    niveau_travaux  VARCHAR(30)   NOT NULL,
    prix_min_m2     NUMERIC(10, 2) NOT NULL,
    prix_max_m2     NUMERIC(10, 2) NOT NULL,
    UNIQUE (organisation_id, type_piece, niveau_travaux)
);

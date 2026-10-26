-- Lots (corps d'état) propres à chaque organisation. La liste par défaut est créée par l'application.
CREATE TABLE lot (
    id              UUID PRIMARY KEY,
    organisation_id UUID         NOT NULL REFERENCES organisation (id),
    code            VARCHAR(20)  NOT NULL,
    nom             VARCHAR(255) NOT NULL,
    ordre           INTEGER      NOT NULL,
    UNIQUE (organisation_id, code)
);

-- Pièces (zones) d'une affaire : cuisine, SDB, chambre 1…
CREATE TABLE piece (
    id         UUID PRIMARY KEY,
    affaire_id UUID         NOT NULL REFERENCES affaire (id) ON DELETE CASCADE,
    nom        VARCHAR(255) NOT NULL,
    surface_m2 NUMERIC(8, 2),
    ordre      INTEGER      NOT NULL
);

CREATE INDEX piece_affaire_idx ON piece (affaire_id);

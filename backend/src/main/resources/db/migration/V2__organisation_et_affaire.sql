CREATE TABLE organisation (
    id         UUID PRIMARY KEY,
    nom        VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Nullable pour les utilisateurs créés avant cette migration : rattachés à leur prochaine connexion.
ALTER TABLE app_user ADD COLUMN organisation_id UUID REFERENCES organisation (id);

CREATE TABLE affaire (
    id               UUID PRIMARY KEY,
    organisation_id  UUID         NOT NULL REFERENCES organisation (id),
    nom              VARCHAR(255) NOT NULL,
    client_nom       VARCHAR(255),
    client_email     VARCHAR(320),
    client_telephone VARCHAR(50),
    adresse          VARCHAR(500),
    type_bien        VARCHAR(30)  NOT NULL,
    statut           VARCHAR(30)  NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX affaire_organisation_idx ON affaire (organisation_id);

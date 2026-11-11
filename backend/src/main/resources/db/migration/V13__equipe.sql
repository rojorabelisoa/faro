-- Plusieurs utilisateurs par entreprise : rôle de chacun et invitations.
ALTER TABLE app_user ADD COLUMN role_entreprise VARCHAR(30) NOT NULL DEFAULT 'GERANT';

CREATE TABLE invitation (
    id              UUID PRIMARY KEY,
    organisation_id UUID         NOT NULL REFERENCES organisation (id) ON DELETE CASCADE,
    code            VARCHAR(64)  NOT NULL UNIQUE,
    role_entreprise VARCHAR(30)  NOT NULL,
    email           VARCHAR(320),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at         TIMESTAMP WITH TIME ZONE,
    used_by         UUID REFERENCES app_user (id) ON DELETE SET NULL
);

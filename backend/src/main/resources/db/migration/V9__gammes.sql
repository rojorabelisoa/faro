-- Options Eco / Standard / Premium : une ligne peut appartenir à une gamme (sinon elle est commune),
-- et chaque pièce (ou l'affaire, pour les lignes générales) a une gamme choisie.
ALTER TABLE ligne_devis ADD COLUMN gamme VARCHAR(20);
ALTER TABLE piece ADD COLUMN gamme_choisie VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE affaire ADD COLUMN gamme_choisie VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

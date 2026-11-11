-- Offre de chaque entreprise (en attendant un paiement en ligne) : essai de 30 jours au départ.
ALTER TABLE organisation ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'ESSAI';
ALTER TABLE organisation ADD COLUMN essai_fin TIMESTAMP WITH TIME ZONE;
UPDATE organisation SET essai_fin = created_at + INTERVAL '30 days';

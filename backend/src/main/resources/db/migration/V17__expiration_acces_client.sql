-- Espace client : les liens expirent (durée par défaut 90 jours, prolongeable par l'entreprise).
-- Liens existants : 90 jours à partir de cette migration.
ALTER TABLE acces_client ADD COLUMN expire_at TIMESTAMP WITH TIME ZONE;
UPDATE acces_client SET expire_at = now() + INTERVAL '90 days';
ALTER TABLE acces_client ALTER COLUMN expire_at SET NOT NULL;

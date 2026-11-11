-- Métrés d'une pièce : avec la surface au sol, on en déduit murs, plafond et périmètre.
ALTER TABLE piece ADD COLUMN perimetre_ml NUMERIC(8, 2);
ALTER TABLE piece ADD COLUMN hauteur_m NUMERIC(5, 2);

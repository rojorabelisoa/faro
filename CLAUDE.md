# Instructions pour Claude

## Reprise automatique du travail

**Avant toute autre action dans une nouvelle session :**

1. Lire `PROGRESS.md` en entier.
2. Reprendre la tâche marquée « En cours », sinon la première tâche non cochée de « À faire ».
3. Ne pas refaire ce qui est déjà coché dans « Fait ».

## Règles de travail

- Découper le travail en petites étapes (une étape = un commit).
- Après chaque étape terminée :
  1. mettre à jour `PROGRESS.md` (cocher, noter l'étape suivante, les décisions prises) ;
  2. commiter `PROGRESS.md` avec le code dans le même commit ;
  3. pousser sur la branche de travail (`git push -u origin <branche>`).
- Avant de commencer une étape longue, noter dans « En cours » ce qui va être fait et dans quels fichiers, pour qu'une session interrompue puisse reprendre exactement là.
- Toute décision importante (choix technique, convention) va dans la section « Décisions » de `PROGRESS.md`.
- Toute question ouverte pour l'utilisateur va dans « Bloquants / questions ».

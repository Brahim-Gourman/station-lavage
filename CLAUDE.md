# Règles du projet Station de lavage

Projet de contrôle de Java avancé, fait par 3 étudiants. Chacun doit pouvoir expliquer chaque ligne de sa partie au professeur.

Sources : `docs/enonce.pdf` et `docs/cahier-des-charges.md`.

## Règles pour tout le projet

- Java 21 et Maven. Aucune bibliothèque sauf H2 (pilote JDBC) et JavaFX.
- Code le plus simple possible, niveau étudiant : pas de framework, pas de Lombok, pas de design pattern inutile, pas de FXML.
- Méthodes courtes (20 lignes maximum), noms en français, un commentaire court en français au-dessus de chaque classe et de chaque méthode publique.
- Ne faire que ce que demande le cahier des charges. Si un point est flou, choisir la solution la plus simple et le dire.
- Les recherches et statistiques utilisent uniquement les Streams, sans boucle.
- Les paquets model, exception, dao, io, service et simulation ne dépendent jamais de console ni de ui.
- Ne jamais faire de commit : les étudiants commitent eux-mêmes.

## Méthode de travail

- Vérifier que `mvn compile` passe à la fin de chaque étape.
- À la fin de chaque jalon : lister les fichiers créés, résumer ce qu'ils font, et attendre l'accord du groupe avant de continuer.

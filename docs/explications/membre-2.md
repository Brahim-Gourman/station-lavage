# Membre 2 — service et console

Ta partie : les règles métier, les recherches et statistiques en Streams, et le menu console (F1 à F10, avec F9 branché sur la simulation).

---

## 1. Paquet `service` : les règles métier

Les services sont entre l'interface (console ou JavaFX) et les DAO. **Toutes les règles sont ici**, donc la console et JavaFX n'en copient aucune.

### `Services` : l'assemblage
- Son constructeur appelle `Connexion.initialiserBase()`, crée les 5 DAO, puis chaque service en lui passant les DAO dont il a besoin (on passe les objets au constructeur au lieu de les créer à l'intérieur).
- Ses attributs sont `public final` : `services.clients`, `services.lavages`, `services.stock`…
- Il est créé **une seule fois** (dans `Main` ou dans `ApplicationFx`). Ainsi, il n'y a qu'**un seul** `StockService`, ce qui est indispensable pour que `synchronized` protège vraiment le stock.
- Il crée aussi le `SimulationService` (partie du membre 3).

### `Verification`
- `texteNonVide(texte, champ)` et `positif(nombre, champ)` lèvent `IllegalArgumentException` avec un message clair. Elles servent dans plusieurs services, ce qui évite de copier les mêmes `if`.

### `ClientService`, `VehiculeService`, `LaveurService` (F1, F2, F3)
- `ajouter`, `modifier`, `supprimer`, `trouver`, `lister`. Chaque action écrit une ligne dans le journal.
- `trouver(id)` : `dao.trouverParId(id).orElseThrow(() -> new EntiteIntrouvableException("Client", id))`. L'`Optional` vide devient une exception métier.
- **Règle de suppression** : on regarde s'il existe un lavage lié avec `anyMatch` :
  ```java
  boolean aDesLavages = lavageDao.lister().stream()
          .anyMatch(l -> l.getVehicule().getClientId() == id);
  ```
  Si oui, on lève `IllegalStateException` (« gardé pour l'historique »). Pour un laveur, le message conseille de le désactiver.
- `ClientService.trouver()` remplit aussi le `Set<Vehicule>` du client avec un Stream : `filter(v -> v.getClientId() == id).forEach(client::ajouterVehicule)`.
- `VehiculeService.ajouter()` et `modifier()` vérifient que le client propriétaire existe (`verifierClient`).
- `LaveurService.activer()` et `desactiver()` appellent `laveur.activer()` ou `desactiver()`, puis `laveurDao.modifier()`.

### `StockService` (F4 et stock partagé)
- `ajouter`, `modifier` (nom et seuil seulement), `supprimer`, `trouver`, `lister`.
- `reapprovisionner(id, quantite)` est `synchronized` : elle refuse une quantité ≤ 0, puis ajoute au stock.
- **`consommer(Formule)` est `synchronized`** :
  1. `produitsParNom()` lit tous les produits et les range dans une `Map<String, Produit>` (`Collectors.toMap(Produit::getNom, Function.identity())`) ;
  2. un Stream cherche le premier produit dont le stock est insuffisant (`filter(...)` puis `findFirst()`) ;
  3. s'il y en a un, elle lève `StockInsuffisantException` **sans rien retirer** ;
  4. sinon, `formule.getConsommation().forEach((nom, quantite) -> retirer(...))` retire chaque produit et l'enregistre en base.
- Comme la méthode est `synchronized`, la vérification et le retrait forment **un seul bloc** : un autre thread ne peut pas retirer du stock entre les deux. Sans cela, deux laveurs pourraient voir « 200 ml restants », retirer chacun 200 ml, et le stock passerait à −200.
- `stockParNom()` renvoie `Map<String, Integer>` (nom → ml). La simulation s'en sert pour le bilan.

### `LavageService` (F5 et F6)
- `creer(vehiculeId, formule, options)` :
  1. trouve le véhicule (sinon `EntiteIntrouvableException`) ;
  2. si `!vehicule.accepte(formule)`, lève `FormuleNonDisponibleException` ;
  3. crée le `Lavage` avec la remise donnée par `calculerRemise()` ;
  4. ajoute les options (`options.forEach(lavage::ajouterOption)`) ;
  5. `lavage.setMontant(lavage.calculerMontant())`, puis l'enregistre en base et dans le journal.
- `calculerRemise(clientId)` compte avec un Stream les lavages `TERMINE` du client. Si ce nombre est ≥ `SEUIL_FIDELITE` (10), la remise vaut `REMISE_FIDELITE` (0.10).
- `attribuerLaveur()` : le lavage doit être `EN_ATTENTE` et le laveur actif.
- `demarrer()` :
  1. le lavage doit être `EN_ATTENTE` ;
  2. `verifierLaveurLibre()` exige un laveur, et qu'aucun autre lavage `EN_COURS` n'ait ce laveur (`anyMatch`) ;
  3. elle appelle `stockService.consommer()` ;
  4. si le stock manque, le lavage passe à `ANNULE`, puis l'exception est relancée (`throw e`) pour que l'interface affiche le message ;
  5. sinon, le lavage passe à `EN_COURS`.
- `terminer()` : le lavage doit être `EN_COURS`. Elle enregistre la date de fin et passe le lavage à `TERMINE`.
- `annuler()` : le lavage doit être `EN_ATTENTE` (« tant qu'il n'est pas commencé »).
- `verifierStatut(lavage, attendu)` regroupe la vérification de toutes les transitions. `changerStatut()` change le statut, enregistre en base et écrit au journal.
- `lister()` renvoie une `List<Lavage>` (on garde l'ordre de création).

### `RechercheService` (F7) : uniquement des Streams
| Méthode | Stream |
|---------|--------|
| `clientsParNom(texte)` | `filter(c -> c.getNom().toLowerCase().contains(cherche))` puis `toList()` |
| `clientsParTelephone(texte)` | `filter(c -> c.getTelephone().contains(texte))` |
| `vehiculeParImmatriculation(immat)` | `filter(equalsIgnoreCase)` puis `findFirst()`, qui renvoie un `Optional<Vehicule>` |
| `lavagesParDate(jour)` | `filter(l -> l.getDateCreation().toLocalDate().equals(jour))` |
| `lavagesParStatut(statut)` | `filter(l -> l.getStatut() == statut)` (la file d'attente = `EN_ATTENTE`) |
| `lavagesParLaveur(id)` | `filter(l -> l.getLaveur() != null && l.getLaveur().getId() == id)` |

### `StatistiqueService` (F8) : uniquement des Streams
- `lavagesTermines()` est la base des calculs de CA : seuls les lavages `TERMINE` sont de l'argent encaissé.
- `caTotal()` : `mapToDouble(Lavage::getMontant).sum()`.
- `caParTypeVehicule()` :
  ```java
  .collect(Collectors.groupingBy(l -> l.getVehicule().getType(),
                                 Collectors.summingDouble(Lavage::getMontant)));
  ```
  `groupingBy` crée une case par type (MOTO, VOITURE, CAMION), et `summingDouble` additionne les montants de chaque case. Le résultat est une `Map<String, Double>`.
- `caParFormule()` : même principe, groupé par `Lavage::getFormule`, donc une `Map<Formule, Double>`.
- `lavagesParLaveur()` : `groupingBy(nom du laveur, Collectors.counting())`, donc une `Map<String, Long>`.
- `meilleurLaveur()` : `lavagesParLaveur().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)`, qui renvoie un `Optional<String>` (vide s'il n'y a aucun lavage).
- `formuleLaPlusDemandee()` : `groupingBy` + `counting` sur **tous** les lavages, puis `max`.
- `panierMoyen()` : `mapToDouble(...).average().orElse(0)`.
- `produitsSousSeuil()` : `filter(Produit::estSousSeuil)`.

---

## 2. Paquet `console` : le menu (phase 1)

### `Main` (paquet racine)
`new MenuConsole(new Services()).lancer();`

### `Saisie` : la lecture sécurisée
- Un seul `Scanner(System.in)` pour tout le programme.
- `lireLigne()` : si l'entrée est fermée (`!hasNextLine()`), le programme s'arrête proprement au lieu de lever `NoSuchElementException`.
- `lireEntier()` : boucle `while (true)` avec `Integer.parseInt` dans un `try`. Si la saisie n'est pas un nombre (`NumberFormatException`), elle affiche un message et **redemande**.
- `lireEntierEntre(min, max)`, `lireTexte()` (non vide), `lireOuiNon()`, `lireDate()` (`LocalDate.parse`, qui redemande en cas de `DateTimeParseException`).
- `afficherListe(List<?>)` affiche chaque élément, ou « (aucun résultat) ». `List<?>` veut dire « une liste de n'importe quel type » (joker des génériques).

### `MenuConsole`
- `lancer()` : boucle qui affiche le menu, lit le choix et appelle `ouvrir(choix)` **dans un `try/catch (Exception e)`**. Toute erreur (métier, base, fichier) s'affiche « Erreur : … » et le menu revient. **Le programme ne plante jamais.**
- `ouvrir(choix)` : un `switch` avec des flèches (`case 1 -> ...`) envoie vers le bon sous-menu.
- `simuler()` (F9) : lit le nombre de laveurs (1 à 20) et de lavages (1 à 500), appelle `services.simulation.lancer()` et affiche `bilan.texte()`.

### `MenuGestion` (F1 à F4)
- Un sous-menu par entité. Chaque choix appelle directement le service.
- `ajouterVehicule()` demande le type, puis crée `new Moto`, `new Voiture` ou `new Camion` avec un `switch` qui renvoie une valeur.
- `modifierVehicule()` utilise `instanceof Voiture voiture` (pattern matching) pour ne demander « SUV ? » qu'aux voitures.

### `MenuLavages` (F5 et F6)
- `enregistrer()` : choix du véhicule, de la formule (`choisirFormule()`) et des options (`choisirOptions()`), puis appel de `services.lavages.creer(...)`. Le montant est calculé par le service, **jamais** par la console.
- `choisirOptions()` met les choix dans un `HashSet`. Si `add()` renvoie `false`, l'option était déjà choisie.
- `suivre()` affiche la file d'attente et les lavages en cours, puis attribue, démarre, termine ou annule.

### `MenuRapports` (F7, F8, F10)
- `rechercher()` : les six recherches de `RechercheService`.
- `statistiques()` : affiche toutes les méthodes de `StatistiqueService`.
- `exporter()` : CSV, ticket et lecture du journal (paquet `io`).

---

## 3. Où sont les notions du module dans ta partie

| Notion | Endroit exact |
|--------|---------------|
| Streams sans boucle | `RechercheService` (toutes les méthodes), `StatistiqueService` (toutes), `LavageService.calculerRemise()`, `StockService.consommer()`, `ClientService.supprimer()` |
| `groupingBy` + `summingDouble` | `StatistiqueService.caParTypeVehicule()`, `caParFormule()` |
| `groupingBy` + `counting` | `StatistiqueService.lavagesParLaveur()`, `formuleLaPlusDemandee()` |
| Lambdas et références de méthode | `l -> l.getStatut() == statut`, `Lavage::getMontant`, `client::ajouterVehicule` |
| `Optional` | `RechercheService.vehiculeParImmatriculation()`, `StatistiqueService.meilleurLaveur()`, `orElseThrow` dans les `trouver()` |
| `synchronized` | `StockService.consommer()`, `StockService.reapprovisionner()` |
| List / Set / Map | `LavageService.lister()` (List), `MenuLavages.choisirOptions()` (Set), `StockService.stockParNom()` (Map) |
| Exceptions | levées dans les services, toutes attrapées dans `MenuConsole.lancer()` |
| Séparation logique / interface | aucun `System.out` dans `service` ; la console n'appelle que les services |

---

## 4. Dix questions que le prof peut poser

**1. Où sont les règles métier, et pourquoi pas dans la console ?**
Dans les services (`LavageService`, `StockService`…). La console ne fait que lire et afficher. Résultat : JavaFX a réutilisé exactement les mêmes services sans copier de règle, comme le demande l'énoncé (« la logique ne dépend jamais de l'interface »).

**2. Explique `caParTypeVehicule()`.**
On part des lavages terminés. `groupingBy(l -> l.getVehicule().getType(), ...)` regroupe les lavages par type de véhicule. Pour chaque groupe, `summingDouble(Lavage::getMontant)` additionne les montants. On obtient une `Map`, par exemple `{VOITURE=115.0, MOTO=21.0}`, sans aucune boucle.

**3. Pourquoi `consommer()` est `synchronized` ? Que se passe-t-il sans ?**
Elle lit le stock, vérifie, puis retire. Sans verrou, deux threads peuvent lire « 300 ml » en même temps, décider tous les deux que c'est suffisant, et retirer chacun 300 ml : le stock devient négatif. `synchronized` fait que la vérification et le retrait s'exécutent d'un bloc, un thread à la fois.

**4. `synchronized` sur une méthode d'instance protège quoi exactement ?**
Le verrou est l'objet `StockService` lui-même (`this`). La protection ne marche donc que si **tous** les threads utilisent la **même** instance. C'est pourquoi `Services` crée un seul `StockService`, partagé par la console, JavaFX et la simulation.

**5. Comment fonctionne la remise de fidélité ?**
`calculerRemise(clientId)` compte avec un Stream les lavages du client qui sont `TERMINE`. S'il y en a au moins 10, la remise vaut 0.10. Elle est fixée à la création du lavage et enregistrée en base, puis `Lavage.calculerMontant()` multiplie par `(1 - remise)`.

**6. Que se passe-t-il si on démarre un lavage alors que le stock manque ?**
`consommer()` lève `StockInsuffisantException` sans rien retirer. `demarrer()` l'attrape, passe le lavage à `ANNULE`, l'enregistre, puis relance l'exception pour que l'interface affiche le message. Le stock reste inchangé, comme le veut le cahier des charges.

**7. Comment la console évite-t-elle de planter sur une mauvaise saisie ?**
À deux niveaux. `Saisie.lireEntier()` attrape `NumberFormatException` et redemande. Toutes les autres erreurs (id inconnu, règle métier, erreur SQL) remontent jusqu'au `try/catch` de `MenuConsole.lancer()`, qui affiche le message et revient au menu.

**8. Pourquoi les services renvoient des `Optional` ou lèvent `EntiteIntrouvableException` au lieu de renvoyer `null` ?**
`null` provoque des `NullPointerException` loin de la cause. `Optional` oblige à traiter le cas « absent » (`orElse`, `orElseThrow`, `isPresent`). Dans les services, on le transforme en exception métier avec un message clair (« Lavage 2 introuvable »).

**9. Différence entre `List`, `Set` et `Map` dans ton code ?**
`List` (`lister()`) garde l'ordre et accepte les doublons : c'est l'historique des lavages. `Set` refuse les doublons : les options choisies dans `choisirOptions()`. `Map` associe une clé à une valeur : `stockParNom()` (nom → ml) et les résultats des statistiques.

**10. Quelles transitions de statut sont autorisées, et où est-ce vérifié ?**
EN_ATTENTE → EN_COURS (`demarrer`), EN_COURS → TERMINE (`terminer`) et EN_ATTENTE → ANNULE (`annuler`, ou automatiquement si le stock manque). Chaque méthode appelle `verifierStatut(lavage, attendu)`, qui lève `IllegalStateException` si le statut n'est pas le bon. Par exemple, on ne peut pas terminer un lavage en attente.

**Questions bonus :**
- *Pourquoi `IllegalStateException` et pas une exception à vous ?* Pour rester simple : c'est l'exception standard de Java pour « action impossible dans l'état actuel ». Les trois exceptions demandées par le cahier des charges existent bien, pour les cas métier principaux.
- *Différence entre `toList()` et `collect(Collectors.toList())` ?* `toList()` (Java 16+) est plus court et renvoie une liste non modifiable.

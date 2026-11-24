# Membre 3 — simulation, ui et README

Ta partie : la simulation multi-threads (F9), les trois écrans JavaFX et le README final.

---

## 1. Paquet `simulation`

### Le principe
N laveurs (N threads) partagent trois ressources : **une file de lavages**, **le stock** et **le journal**. À la fin, un bilan prouve qu'aucun lavage n'est perdu ou traité deux fois, et que le stock est juste.

### `SimulationService`
- Il reçoit les services existants (`LavageService`, `LaveurService`, `ClientService`, `VehiculeService`, `RechercheService`, `StockService`) et le `LavageDao`. La simulation **réutilise les mêmes règles** : les lavages sont créés par `LavageService.creer()`, avec le vrai prix et la vraie remise.
- `lancer(nbLaveurs, nbLavages)` enchaîne :
  1. une vérification : au moins 1 laveur et 1 lavage ;
  2. `preparerLaveurs(n)` : `IntStream.rangeClosed(1, n).mapToObj(...)` réutilise ou crée « Laveur sim 1…N » ;
  3. `preparerVehicule()` : réutilise ou crée la voiture « SIM-0001 » et son client ;
  4. elle note `stockInitial = stockService.stockParNom()` **avant** de démarrer ;
  5. `remplirFile()` : crée les lavages (Express, Complet, Premium à tour de rôle, avec `i % 3`) et les met dans une `LinkedBlockingQueue<Lavage>`. **La file est pleine avant le démarrage des threads** ;
  6. `executer(file, laveurs)` lance les threads et attend qu'ils finissent ;
  7. `faireBilan()` calcule le bilan.
- `executer()` :
  ```java
  ExecutorService executeur = Executors.newFixedThreadPool(laveurs.size());
  List<TacheLaveur> taches = laveurs.stream().map(l -> new TacheLaveur(file, l, stockService, lavageDao)).toList();
  taches.forEach(executeur::execute);
  executeur.shutdown();                         // plus aucune nouvelle tâche acceptée
  executeur.awaitTermination(10, TimeUnit.MINUTES); // attend la fin de toutes les tâches
  ```
  - `newFixedThreadPool(N)` crée un groupe de N threads réutilisables.
  - `execute()` confie une tâche (`Runnable`) à un thread du groupe.
  - `shutdown()` ne coupe rien : il dit « plus de nouvelle tâche ». `awaitTermination()` bloque jusqu'à la fin des tâches.
- `faireBilan()` **relit chaque lavage en base** (`lavageDao.trouverParId`). Le bilan vient donc de ce qui est vraiment enregistré, pas de ce qu'on croit avoir fait. Il compte les TERMINE et les ANNULE, additionne `getNbPris()` de chaque tâche, et calcule la consommation.
- `consommation()` : `flatMap` transforme chaque lavage terminé en ses lignes de consommation (shampooing → 300, cire → 100…), puis `groupingBy` + `summingInt` les additionne par produit.

### `TacheLaveur` (implements `Runnable`)
- Ses attributs : la file **partagée**, **son** laveur, le `StockService` **partagé**, le `LavageDao`, et un compteur `nbPris`.
- `run()` :
  ```java
  Lavage lavage = file.poll();
  while (lavage != null) {
      nbPris++;
      traiter(lavage);
      lavage = file.poll();
  }
  ```
  - `poll()` retire et renvoie la tête de la file, ou `null` si elle est vide. Une `BlockingQueue` est **thread-safe** : chaque lavage n'est remis qu'à **un seul** thread.
  - Quand la file est vide, `poll()` renvoie `null`, la boucle s'arrête et le thread a fini.
  - On utilise `poll()` et pas `take()`, parce que `take()` attendrait **pour toujours** un nouvel élément alors que la file est déjà complète dès le départ.
- `traiter(lavage)` :
  1. attribue le laveur ;
  2. appelle `stockService.consommer(formule)`, qui est **synchronized**. Si elle lève `StockInsuffisantException`, le lavage passe à ANNULE et on s'arrête là ;
  3. le lavage passe à EN_COURS, puis `Thread.sleep(durée × 10 ms)` simule le travail (1 minute simulée = 10 ms) ;
  4. le lavage passe à TERMINE avec la date de fin.
- `enregistrer()` : `lavageDao.modifier()` (une connexion par opération) et `Journal.ecrire()` (`synchronized`).
- `InterruptedException` : si le thread est interrompu, on remet le drapeau (`Thread.currentThread().interrupt()`) et on s'arrête proprement.
- Chaque laveur est un seul thread qui traite ses lavages l'un après l'autre : la règle « un laveur = un lavage à la fois » est donc respectée.

### `BilanSimulation` (record)
- `record` (Java 16+) : une petite classe **immuable**. Java génère le constructeur, les accesseurs (`crees()`, `termines()`…), `equals`, `hashCode` et `toString`.
- Ses trois contrôles sont écrits avec des Streams :
  - `aucunLavagePerdu()` : `termines + annules == crees` **et** `prisDansLaFile == crees` (aucun lavage pris deux fois ni oublié) ;
  - `stockJuste()` : pour chaque produit, `stockFinal == stockInitial − consommation` (`allMatch`) ;
  - `aucunStockNegatif()` : `allMatch(quantite -> quantite >= 0)`.
- `texte()` construit le bilan affiché, le même pour la console et pour JavaFX.

### Ressources partagées et protections
| Ressource | Risque | Protection |
|-----------|--------|------------|
| File de lavages | deux laveurs prennent le même | `BlockingQueue.poll()` est atomique |
| Stock | stock négatif | `StockService.consommer()` est `synchronized` (instance unique) |
| Base de données | `Connection` partagée | une connexion par opération dans chaque méthode du DAO |
| Journal | lignes mélangées | `Journal.ecrire()` est `static synchronized` |
| Compteur `nbPris` | — | il n'est pas partagé : chaque tâche a le sien, et on le lit après `awaitTermination()` |

### Résultat de l'essai (5 fois, 4 laveurs, 30 lavages)
Les 5 bilans sont justes. Pendant le 3e lancement, le stock de shampooing s'épuise : 15 lavages sont terminés et 15 annulés, et il reste 100 ml, jamais en négatif. Les 4e et 5e lancements annulent tout et le stock ne bouge pas. Le journal montre que les 4 laveurs se sont partagé le travail (20, 18, 17 et 20 lavages).

---

## 2. Paquet `ui` : JavaFX (sans FXML)

### `ApplicationFx` (extends `Application`)
- `start(Stage fenetre)` crée **un seul** `Services` (les mêmes classes que la console), les trois contrôleurs et un `TabPane` à trois onglets.
- `setOnSelectionChanged(e -> controleur.rafraichir())` recharge les données à chaque changement d'onglet.
- `main()` appelle `launch(args)`, qui démarre JavaFX et appelle `start()` sur le **thread JavaFX**.

### `OutilsFx`
- `Action` est une **interface imbriquée** avec une méthode `executer() throws Exception`. On ne peut pas utiliser `Runnable`, car sa méthode `run()` ne peut pas lever d'exception vérifiée.
- `bouton(texte, action)` crée un bouton dont le clic appelle `essayer(action)`.
- `essayer(action)` exécute l'action dans un `try/catch` et affiche une `Alert` d'erreur avec le message. L'interface ne plante donc jamais, comme la console.
- `info()` et `titre()` sont de petits utilitaires d'affichage.

### `ClientsController` : écran « Clients et véhicules »
- À gauche : champ de recherche, `ListView<Client>`, champs nom et téléphone, et les boutons Ajouter, Modifier, Supprimer.
- À droite : `ListView<Vehicule>` du client choisi, et un formulaire (type, immatriculation, SUV, essieux).
- `selectedItemProperty().addListener(...)` : quand on clique sur un client, `choisirClient()` remplit le formulaire et affiche ses véhicules (via `services.clients.trouver()`).
- `rechercher()` : si le texte commence par un chiffre, on cherche par téléphone, sinon par nom (les deux recherches sont dans `RechercheService`).
- Tous les boutons appellent seulement `services.clients` ou `services.vehicules`.

### `LavagesController` : écran « Lavages »
- À gauche : choix du véhicule, de la formule et des options, puis « Créer le lavage ». `creer()` transforme les cases cochées en `Set<Option>` avec un Stream et appelle `services.lavages.creer()`. Le **montant affiché vient du service**.
- Chaque case à cocher garde son option avec `setUserData(option)`.
- À droite : filtre de statut (EN_ATTENTE = la file d'attente, ou TOUS), liste des lavages, choix du laveur, et les boutons Attribuer, Démarrer, Terminer, Annuler et Ticket.
- `boutonSuivi()` recharge la liste **dans un `finally`**, même si l'action a échoué. Par exemple, un démarrage sans stock passe le lavage à ANNULE puis lève une exception : la liste doit montrer ce changement.

### `TableauBordController` : écran « Tableau de bord »
- À gauche : toutes les statistiques de `StatistiqueService` dans une `TextArea`, avec un bouton « Actualiser ».
- À droite : deux `Spinner` (laveurs, lavages), le bouton « Lancer la simulation », un `ProgressIndicator` et le bilan.
- **La simulation tourne hors du thread JavaFX** :
  ```java
  private void lancerSimulation() {          // appelé sur le thread JavaFX (clic)
      ...
      Thread fil = new Thread(() -> simulerHorsInterface(laveurs, lavages));
      fil.setDaemon(true);
      fil.start();
  }

  private void simulerHorsInterface(int laveurs, int lavages) {   // autre thread
      BilanSimulation bilan = services.simulation.lancer(laveurs, lavages);
      Platform.runLater(() -> finirSimulation(bilan.texte() + verdict));
  }
  ```
  - Si on lançait la simulation directement dans le clic, le thread JavaFX serait bloqué plusieurs secondes : la fenêtre ne se redessinerait plus.
  - Un composant JavaFX ne doit être modifié **que** depuis le thread JavaFX. `Platform.runLater(...)` demande à JavaFX d'exécuter `finirSimulation()` sur son thread dès que possible.
  - `setDaemon(true)` : si on ferme la fenêtre, ce thread n'empêche pas le programme de s'arrêter.
  - Le bouton est désactivé pendant la simulation, ce qui évite deux lancements en même temps.

---

## 3. README

Le README contient tout ce que demande l'énoncé :
1. le sujet ;
2. le diagramme de classes à jour (Mermaid) ;
3. les instructions de lancement (`mvn compile exec:java`, `mvn compile javafx:run`) et le script SQL ;
4. le tableau des exigences techniques, avec la classe et la méthode de chaque notion ;
5. la répartition du travail.

Il liste aussi les choix faits là où le cahier des charges était flou.

---

## 4. Où sont les notions du module dans ta partie

| Notion | Endroit exact |
|--------|---------------|
| Threads, ExecutorService | `SimulationService.executer()` |
| BlockingQueue | `SimulationService.remplirFile()`, `TacheLaveur.run()` (`poll()`) |
| Runnable | `TacheLaveur implements Runnable` |
| synchronized (utilisé) | `StockService.consommer()` et `Journal.ecrire()`, appelés depuis `TacheLaveur.traiter()` |
| Thread.sleep | `TacheLaveur.traiter()` |
| Streams | `SimulationService.consommation()` (`flatMap`, `groupingBy`, `summingInt`), `preparerLaveurs()` (`IntStream`), `BilanSimulation` (`allMatch`), `LavagesController.creer()` |
| record | `BilanSimulation` |
| Interface imbriquée, lambdas | `OutilsFx.Action`, `OutilsFx.bouton(..., this::ajouterClient)` |
| JavaFX | `ApplicationFx`, `ClientsController`, `LavagesController`, `TableauBordController` |
| Thread hors JavaFX + `Platform.runLater` | `TableauBordController.lancerSimulation()`, `simulerHorsInterface()` |

---

## 5. Dix questions que le prof peut poser

**1. Comment es-tu sûr qu'un lavage n'est pas traité par deux laveurs ?**
Les lavages sont dans une `LinkedBlockingQueue`, une file thread-safe. `poll()` retire l'élément de façon atomique : un seul thread peut le recevoir. Le bilan le vérifie : la somme des `nbPris` de toutes les tâches est égale au nombre de lavages créés, et terminés + annulés = créés.

**2. Pourquoi `poll()` et pas `take()` ?**
`take()` bloque et attend si la file est vide. Ici, la file est entièrement remplie **avant** le démarrage des laveurs, donc une file vide veut dire « travail fini ». `poll()` renvoie alors `null`, ce qui arrête la boucle. Avec `take()`, les threads attendraient pour toujours et `awaitTermination` ne finirait jamais.

**3. Que font `shutdown()` et `awaitTermination()` ?**
`shutdown()` interdit d'ajouter de nouvelles tâches, mais laisse finir celles en cours. `awaitTermination(10, MINUTES)` bloque le thread appelant jusqu'à ce que toutes les tâches soient terminées (ou que le délai soit dépassé). C'est seulement après qu'on calcule le bilan.

**4. Pourquoi le stock ne peut-il pas devenir négatif ?**
Chaque tâche appelle `StockService.consommer()`, qui est `synchronized` sur l'unique instance de `StockService`. La vérification et le retrait se font d'un bloc, un thread à la fois. Si le stock manque, rien n'est retiré et le lavage est annulé. Le bilan contrôle aussi `stockFinal == stockInitial − consommation` et qu'aucune valeur n'est négative.

**5. Pourquoi ne pas partager une seule `Connection` entre les threads ?**
Une `Connection` JDBC n'est pas faite pour être utilisée par plusieurs threads en même temps : les requêtes pourraient se mélanger. Chaque méthode du DAO ouvre sa propre connexion dans un try-with-resources et la ferme aussitôt.

**6. Pourquoi la simulation tourne dans un autre thread dans JavaFX ?**
Le thread JavaFX dessine la fenêtre et traite les clics. S'il calculait la simulation pendant plusieurs secondes, la fenêtre serait gelée. On lance donc un `new Thread(...)`, et le thread JavaFX reste libre (l'indicateur de progression continue de tourner).

**7. À quoi sert `Platform.runLater()` ?**
On ne peut modifier un composant JavaFX (`TextArea`, `Button`…) que depuis le thread JavaFX. Le thread de simulation ne peut donc pas écrire directement le bilan. `Platform.runLater(() -> finirSimulation(...))` place ce code dans la file d'attente du thread JavaFX, qui l'exécute dès que possible.

**8. Comment les écrans évitent-ils de dupliquer les règles métier ?**
Ils n'appellent que les services : `services.lavages.creer()` calcule le prix et la remise, `services.lavages.demarrer()` vérifie le statut et le stock, etc. Les écrans ne font que lire les champs et afficher les résultats. Les exceptions des services sont affichées par `OutilsFx.essayer()`.

**9. Qu'est-ce qu'un `record`, et pourquoi pour le bilan ?**
Un record est une classe immuable dont Java génère le constructeur, les accesseurs, `equals`, `hashCode` et `toString`. Le bilan est un simple résultat qui ne change plus une fois calculé : le record est la forme la plus courte et la plus sûre.

**10. Le bilan est calculé à partir de quoi ? Pourquoi relire la base ?**
`faireBilan()` relit chaque lavage avec `lavageDao.trouverParId()` et relit le stock avec `stockService.stockParNom()`. On contrôle ainsi ce qui est **vraiment enregistré**, pas les objets en mémoire. Si un thread avait oublié d'enregistrer, le contrôle le verrait.

**Questions bonus :**
- *Pourquoi `OutilsFx.Action` au lieu de `Runnable` ?* `Runnable.run()` ne peut pas lever d'exception vérifiée, alors que les services en lèvent (`EntiteIntrouvableException`…). Notre interface déclare `throws Exception`.
- *Pourquoi pas de FXML ?* C'est une consigne du groupe : tout est écrit en Java, ce qui est plus simple à expliquer ligne par ligne.
- *Que montre le journal après une simulation ?* Une ligne par étape (démarre, termine ou annule), écrite par des threads différents mais jamais mélangée, grâce à `Journal.ecrire()` synchronized.

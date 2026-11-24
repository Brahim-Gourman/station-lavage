# Membre 1 — model, exception, dao, io

Ta partie : les objets métier, les erreurs métier, l'accès à la base H2 et les fichiers (CSV, ticket, journal), ainsi que le script `sql/creation_base.sql`.

---

## 1. Paquet `model` : les objets métier

### `Facturable` (interface)
- Une seule méthode : `double calculerMontant()`.
- Elle représente « tout ce qui a un prix à payer ». Deux classes l'implémentent : `Lavage` et `Lavage.Option`.
- Intérêt : on peut appeler `calculerMontant()` sur n'importe quel `Facturable` sans savoir s'il s'agit d'un lavage ou d'une option. C'est du **polymorphisme par interface**.

### `Formule` (enum)
- Trois constantes : `EXPRESS`, `COMPLET`, `PREMIUM`.
- Chaque constante reçoit trois valeurs dans son constructeur : le prix de base, la durée simulée en minutes et une `Map<String, Integer>` de consommation (nom du produit → millilitres).
- Exemple : `PREMIUM(80, 45, Map.of("shampooing", 300, "cire", 100))`.
- `getConsommation()` renvoie cette Map. C'est elle qui dit au stock quoi retirer.
- Une enum peut avoir des attributs, un constructeur (toujours privé) et des méthodes.

### `StatutLavage` (enum)
- `EN_ATTENTE`, `EN_COURS`, `TERMINE`, `ANNULE`. Les transitions autorisées sont vérifiées dans `LavageService` (partie du membre 2).

### `Vehicule` (classe abstraite)
- Attributs : `id`, `clientId` (le propriétaire) et `immatriculation`.
- `calculerPrix(Formule)` est **abstraite** : chaque sous-classe doit l'écrire.
- `getType()` est aussi abstraite. Elle renvoie "MOTO", "VOITURE" ou "CAMION", la valeur stockée dans la colonne `type` de la base.
- `accepte(Formule)` renvoie `true` par défaut ; seule `Moto` la redéfinit.
- `equals()` et `hashCode()` sont redéfinies sur l'immatriculation : deux objets avec la même immatriculation sont « le même véhicule ». C'est indispensable pour que le `Set<Vehicule>` du client refuse les doublons.
- On ne peut pas écrire `new Vehicule(...)` : la classe est abstraite.

### `Moto`, `Voiture`, `Camion` (sous-classes)
| Classe | `calculerPrix(f)` | Particularité |
|--------|-------------------|---------------|
| `Moto` | `f.getPrixBase() * 7 / 10` | redéfinit `accepte()` : seulement `EXPRESS` |
| `Voiture` | prix de base, + 10 si `suv` | attribut `suv` |
| `Camion` | `prix de base * 1.5 + 10 * max(0, nbEssieux - 2)` | attribut `nbEssieux` |

- La Moto calcule `× 7 / 10` au lieu de `× 0.7`, parce que `0.7` n'est pas représentable exactement en binaire. Avec `× 7 / 10`, `30 × 7 = 210` puis `210 / 10 = 21.0` tombent juste.
- Les trois exemples du cahier des charges ont été vérifiés : 21, 90 et 85 MAD.

### `Client`
- `id`, `nom`, `telephone`, et un `Set<Vehicule> vehicules` (`HashSet`).
- `ajouterVehicule(v)` appelle `vehicules.add(v)`. Si le véhicule est déjà présent (même immatriculation), le Set ne l'ajoute pas.

### `Laveur`
- `id`, `nom`, `actif`. `activer()` et `desactiver()` changent le booléen. On désactive un laveur qui part au lieu de le supprimer, pour garder l'historique.

### `Produit`
- `id`, `nom`, `stockMl`, `seuilMl`. `estSousSeuil()` renvoie `stockMl < seuilMl`.

### `Lavage` (implements `Facturable`)
- Attributs : `vehicule`, `laveur` (null au départ), `formule`, `statut` (`EN_ATTENTE` au départ), `dateCreation` (`LocalDateTime.now()`), `dateFin`, `remise`, `montant` et `Set<Option> options`.
- `ajouterOption(o)` ajoute l'option dans un Set : impossible de la prendre deux fois.
- `calculerMontant()` :
  1. additionne les options avec un Stream : `options.stream().mapToDouble(Option::calculerMontant).sum()` ;
  2. ajoute `vehicule.calculerPrix(formule)`. C'est **l'appel polymorphe** : Java exécute la version Moto, Voiture ou Camion selon l'objet réel ;
  3. multiplie par `(1 - remise)` et arrondit au centime avec `Math.round(x * 100) / 100.0`.
- `montant` est la valeur enregistrée en base, calculée une seule fois à la création. `calculerMontant()`, elle, recalcule.

### `Lavage.Option` (classe imbriquée statique)
- Déclarée **à l'intérieur** de `Lavage` avec `public static class Option implements Facturable`.
- **Statique** : elle n'a pas besoin d'un objet `Lavage` pour exister. On écrit `new Lavage.Option("x", 10)`, et non `unLavage.new Option(...)`.
- **Imbriquée** : une option n'a de sens que pour un lavage. Le nom `Lavage.Option` le montre.
- Trois constantes : `DESODORISANT` (10), `CIRAGE_PNEUS` (15), `NETTOYAGE_MOTEUR` (40). `catalogue()` les renvoie dans une `List`.
- `equals()` et `hashCode()` sont redéfinies sur le nom, pour que le Set refuse deux fois la même option.

---

## 2. Paquet `exception`

| Exception | Quand ? | Qui la lève ? |
|-----------|---------|---------------|
| `StockInsuffisantException` | un produit manque au démarrage | `StockService.consommer()` |
| `FormuleNonDisponibleException` | formule refusée par le véhicule (Premium pour une moto) | `LavageService.creer()` |
| `EntiteIntrouvableException` | un id ne correspond à rien | les méthodes `trouver()` des services |

- Les trois héritent de `Exception` : ce sont des **exceptions vérifiées (checked)**. Le compilateur oblige l'appelant à les attraper (`catch`) ou à les déclarer (`throws`). On a fait ce choix parce que ce sont des cas métier normaux que l'appelant doit prévoir.
- `EntiteIntrouvableException(String entite, int id)` construit le message lui-même, par exemple « Client 12 introuvable ».

`BaseDonneesException` est dans le paquet `dao`, car c'est une erreur **technique** et non métier. Elle hérite de `RuntimeException` (non vérifiée) et enveloppe la `SQLException` d'origine (la « cause »). Ainsi, les services ne sont pas obligés d'écrire `throws SQLException` partout.

---

## 3. Paquet `dao` : l'accès à la base (JDBC)

### `Connexion`
- `URL = "jdbc:h2:./data/station;DB_CLOSE_DELAY=-1"` : base H2 en **fichier** (`data/station.mv.db`). `DB_CLOSE_DELAY=-1` garde la base ouverte tant que le programme tourne. Sans cette option, H2 refermerait le fichier à chaque fois que la dernière connexion se ferme, ce qui serait lent.
- `ouvrir()` renvoie `DriverManager.getConnection(URL, "sa", "")`. Chaque appel donne une **nouvelle** connexion.
- `initialiserBase()` exécute `RUNSCRIPT FROM 'sql/creation_base.sql'`, une commande H2 qui lit et exécute le fichier SQL. Elle est appelée une seule fois au démarrage (par `Services`).

### `Dao<T>` (interface générique)
```java
public interface Dao<T> {
    void ajouter(T entite);
    Optional<T> trouverParId(int id);
    List<T> lister();
    void modifier(T entite);
    void supprimer(int id);
}
```
- `T` est un **paramètre de type**. `ClientDao implements Dao<Client>` remplace `T` par `Client` : `ajouter(Client)`, `Optional<Client> trouverParId(int)`, etc.
- Avantage : un seul contrat pour 5 DAO, et le compilateur vérifie les types (impossible de passer un `Laveur` à `ClientDao.ajouter`).
- `Optional<T>` remplace le `null` quand rien n'est trouvé, et oblige l'appelant à traiter le cas « absent ».

### Le schéma commun à toutes les méthodes des DAO
```java
try (Connection c = Connexion.ouvrir();
     PreparedStatement ps = c.prepareStatement("SELECT * FROM client WHERE id = ?")) {
    ps.setInt(1, id);
    ResultSet rs = ps.executeQuery();
    ...
} catch (SQLException e) {
    throw new BaseDonneesException("...", e);
}
```
- **try-with-resources** : `Connection` et `PreparedStatement` sont fermés automatiquement à la fin du bloc, même en cas d'exception. Le `ResultSet` est fermé avec son `PreparedStatement`.
- **Une connexion par opération** : aucune connexion n'est partagée, donc plusieurs threads peuvent utiliser le même DAO en même temps (simulation).
- **PreparedStatement** : les valeurs passent par des `?` et `setString/setInt`. Cela empêche l'**injection SQL**, car une valeur ne peut jamais être lue comme du code SQL.
- `Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys()` récupèrent l'id créé par `AUTO_INCREMENT`, puis `setId()` le met dans l'objet.

### Particularités
- **`VehiculeDao.lire()`** : un `switch` sur la colonne `type` recrée le bon objet (`new Moto`, `new Voiture` ou `new Camion`). C'est la seule façon de retrouver la bonne sous-classe à partir d'une table unique.
- **`VehiculeDao.remplir()`** : les colonnes `suv` et `nb_essieux` sont remplies avec `instanceof` et le *pattern matching* (`v instanceof Voiture voiture && voiture.isSuv()`).
- **`LavageDao`** reçoit `VehiculeDao` et `LaveurDao` dans son constructeur, pour recréer le véhicule et le laveur de chaque lavage.
  - `ajouter()` insère la ligne `lavage`, puis les lignes `lavage_option` (`ajouterOptions`), avec la même connexion.
  - `remplirSuivi(ps, l, debut)` remplit les 7 colonnes communes à INSERT et UPDATE à partir de la position `debut` (2 pour l'INSERT, 1 pour l'UPDATE). Cela évite de dupliquer du code.
  - `lireLaveur()` utilise `rs.wasNull()`, car `getInt()` renvoie 0 pour une colonne NULL.
  - Les dates passent par `setObject` et `getObject(col, LocalDateTime.class)`.
  - Les enums sont stockées par leur nom (`formule.name()`) et relues avec `Formule.valueOf(...)`.

### `sql/creation_base.sql`
- Six tables : `client`, `vehicule`, `laveur`, `produit`, `lavage`, `lavage_option`.
- `CREATE TABLE IF NOT EXISTS` : on peut relancer le script sans erreur.
- `UNIQUE` sur `telephone`, `immatriculation` et le nom du produit.
- Clés étrangères :
  - `vehicule.client_id` est en `ON DELETE CASCADE` : supprimer un client supprime ses véhicules.
  - `lavage_option.lavage_id` est aussi en cascade.
  - `lavage.vehicule_id` et `lavage.laveur_id` ne sont pas en cascade : la base refuse de supprimer un véhicule ou un laveur qui a des lavages.
- `lavage_option` a une clé primaire `(lavage_id, nom)` : la base aussi empêche une option en double.
- Les deux produits de départ sont insérés avec `INSERT ... SELECT ... WHERE NOT EXISTS`, pour ne jamais les ajouter deux fois.

---

## 4. Paquet `io` : fichiers (java.nio)

### `Journal`
- Fichier `logs/journal.log`. `ecrire(type, detail)` ajoute une ligne `date | type | détail`.
- `Files.writeString(fichier, ligne, UTF_8, CREATE, APPEND)` : crée le fichier s'il n'existe pas et écrit **à la fin**.
- `Files.createDirectories(...)` crée le dossier `logs` si besoin.
- `ecrire()` est **`static synchronized`** : le verrou est pris sur la classe `Journal` elle-même. Un seul thread écrit à la fois, donc deux lignes de deux laveurs ne se mélangent jamais.
- L'`IOException` (vérifiée) est transformée en `UncheckedIOException`, pour que les services n'aient pas à la déclarer.
- `lire()` renvoie `Files.readAllLines(...)`, ou une liste vide si le fichier n'existe pas encore.

### `ExportService`
- `exporterLavagesCsv(List<Lavage>)` écrit `exports/lavages.csv` : un en-tête, puis une ligne par lavage, construite avec `lavages.stream().map(this::ligneCsv)`. Le séparateur est `;`, car Excel en français l'attend.
- `genererTicket(Lavage)` écrit `exports/tickets/ticket-<id>.txt` : véhicule, formule, options, remise, statut et montant payé.
- Les deux méthodes utilisent `Files.write(chemin, lignes, UTF_8)` et écrivent une ligne dans le journal.

---

## 5. Où sont les notions du module dans ta partie

| Notion | Endroit exact |
|--------|---------------|
| Héritage, classe abstraite | `Vehicule` (abstract) → `Moto`, `Voiture`, `Camion` |
| Polymorphisme | `Lavage.calculerMontant()` appelle `vehicule.calculerPrix(formule)` ; `Moto.accepte()` |
| Interface | `Facturable`, implémentée par `Lavage` et `Lavage.Option` |
| Classe imbriquée statique | `Lavage.Option` |
| Set | `Client.vehicules`, `Lavage.options`, avec `equals()`/`hashCode()` redéfinies |
| Map | `Formule.getConsommation()` |
| List | `Dao.lister()`, `Lavage.Option.catalogue()` |
| Génériques | `Dao<T>`, `Optional<T>` |
| Stream | `Lavage.calculerMontant()` (somme des options), `ExportService.exporterLavagesCsv()` |
| Exceptions | `exception/*`, `BaseDonneesException` |
| JDBC | `Connexion`, les 5 DAO, `PreparedStatement`, try-with-resources |
| Entrées/sorties | `Journal.ecrire()`, `ExportService.exporterLavagesCsv()`, `ExportService.genererTicket()` |
| synchronized | `Journal.ecrire()` |

---

## 6. Dix questions que le prof peut poser

**1. Pourquoi `Vehicule` est-elle abstraite et pas une interface ?**
Parce qu'elle a des **attributs communs** (`id`, `clientId`, `immatriculation`), un constructeur et du code partagé (`equals`, `hashCode`, `accepte`). Une interface ne peut pas avoir d'attributs d'instance. On ne crée jamais un « véhicule » tout court, toujours une moto, une voiture ou un camion, donc elle est abstraite.

**2. Montre-moi le polymorphisme.**
Dans `Lavage.calculerMontant()`, on écrit `vehicule.calculerPrix(formule)`. Le type déclaré est `Vehicule`, mais Java exécute la méthode de la classe réelle de l'objet : `Moto`, `Voiture` ou `Camion`. C'est la **liaison dynamique**. Pour une moto Express, on obtient 21 ; pour une voiture SUV Premium, 90.

**3. Pourquoi `Option` est une classe imbriquée **statique** ?**
Une option n'existe que dans un lavage, d'où l'imbrication. Mais elle n'a pas besoin d'accéder aux attributs d'un lavage précis, d'où `static`. On peut donc créer les constantes `DESODORISANT`… et `new Lavage.Option(nom, prix)` dans le DAO sans objet `Lavage`. Une classe interne non statique garderait une référence cachée vers un `Lavage`.

**4. Comment empêches-tu de choisir deux fois la même option ?**
`options` est un `Set` (`HashSet`). `Option` redéfinit `equals()` et `hashCode()` sur le nom, donc `add()` d'une option déjà présente ne fait rien. En base, la clé primaire `(lavage_id, nom)` de `lavage_option` le garantit aussi.

**5. Pourquoi redéfinir `hashCode()` en même temps que `equals()` ?**
Un `HashSet` range d'abord l'objet selon son `hashCode()`, puis compare avec `equals()`. Si deux objets égaux avaient des `hashCode` différents, ils seraient rangés à deux endroits et le Set garderait un doublon. La règle : deux objets égaux doivent avoir le même `hashCode`.

**6. À quoi sert le `T` de `Dao<T>` ?**
C'est un paramètre de type. On écrit une seule interface pour tous les DAO, et chaque implémentation choisit son type (`Dao<Client>`, `Dao<Lavage>`…). Le compilateur vérifie les types, et on n'a jamais besoin de cast.

**7. Pourquoi `PreparedStatement` plutôt que concaténer les valeurs dans la requête ?**
Contre l'**injection SQL** : avec `"... WHERE nom = '" + nom + "'"`, un nom comme `' OR '1'='1` changerait la requête. Avec `?` et `setString`, la valeur est toujours traitée comme une donnée. Cela gère aussi les apostrophes et les types (dates, booléens).

**8. Que fait le try-with-resources, et pourquoi une connexion par opération ?**
Les ressources déclarées dans `try (...)` sont fermées automatiquement à la sortie du bloc, même en cas d'exception, donc aucune fuite de connexion. Une connexion par opération, parce qu'un objet `Connection` JDBC ne doit pas être partagé entre threads : pendant la simulation, chaque laveur ouvre ses propres connexions.

**9. Comment le DAO sait-il s'il doit recréer une Moto, une Voiture ou un Camion ?**
Par la colonne `type`, remplie avec `v.getType()` à l'insertion. À la lecture, `VehiculeDao.lire()` fait un `switch` sur cette colonne et appelle le bon constructeur. Les colonnes `suv` et `nb_essieux` servent seulement pour la voiture et le camion.

**10. Pourquoi `Journal.ecrire()` est `synchronized` ? Que se passerait-il sans ?**
Plusieurs threads laveurs écrivent dans le même fichier. Sans `synchronized`, deux écritures pourraient s'entrelacer et produire des lignes mélangées ou coupées. Avec `static synchronized`, un thread qui écrit tient le verrou de la classe, et les autres attendent leur tour.

**Questions bonus :**
- *Checked ou unchecked ?* Les trois exceptions métier sont vérifiées, car l'appelant doit les prévoir. `BaseDonneesException` et `UncheckedIOException` ne le sont pas : ce sont des erreurs techniques qu'on ne peut pas corriger sur place.
- *Pourquoi `* 7 / 10` ?* Pour éviter l'erreur d'arrondi de `0.7` en binaire. En plus, `calculerMontant()` arrondit au centime.

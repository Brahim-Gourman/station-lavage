package station.dao;

import java.util.List;
import java.util.Optional;

/** Opérations communes à tous les DAO. T est le type d'entité : Client, Laveur, Lavage... */
public interface Dao<T> {

    /** Enregistre l'entité et lui donne l'identifiant créé par la base. */
    void ajouter(T entite);

    /** Cherche une entité par son identifiant ; Optional vide si elle n'existe pas. */
    Optional<T> trouverParId(int id);

    /** Retourne toutes les entités. */
    List<T> lister();

    /** Enregistre les changements d'une entité existante. */
    void modifier(T entite);

    /** Supprime l'entité qui a cet identifiant. */
    void supprimer(int id);
}

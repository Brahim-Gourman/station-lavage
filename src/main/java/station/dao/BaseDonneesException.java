package station.dao;

/** Erreur technique de la base de données : elle enveloppe une SQLException. */
public class BaseDonneesException extends RuntimeException {

    /** Crée l'erreur avec un message et la cause d'origine. */
    public BaseDonneesException(String message, Throwable cause) {
        super(message, cause);
    }
}

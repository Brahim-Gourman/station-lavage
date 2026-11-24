package station.exception;

/** Levée quand un produit manque pour démarrer un lavage. */
public class StockInsuffisantException extends Exception {

    /** Crée l'exception avec un message lisible. */
    public StockInsuffisantException(String message) {
        super(message);
    }
}

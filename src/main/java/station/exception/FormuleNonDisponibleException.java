package station.exception;

/** Levée quand on choisit une formule refusée par le véhicule (ex. Premium pour une moto). */
public class FormuleNonDisponibleException extends Exception {

    /** Crée l'exception avec un message lisible. */
    public FormuleNonDisponibleException(String message) {
        super(message);
    }
}

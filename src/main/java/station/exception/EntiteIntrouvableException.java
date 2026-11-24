package station.exception;

/** Levée quand un identifiant ne correspond à rien en base. */
public class EntiteIntrouvableException extends Exception {

    /** Crée l'exception avec un message du type "Client 12 introuvable". */
    public EntiteIntrouvableException(String entite, int id) {
        super(entite + " " + id + " introuvable");
    }
}

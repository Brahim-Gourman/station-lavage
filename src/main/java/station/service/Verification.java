package station.service;

/** Petites vérifications de saisie partagées par les services. */
public class Verification {

    /** Refuse un texte vide ou composé seulement d'espaces. */
    public static void texteNonVide(String texte, String champ) {
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException("Le champ " + champ + " est obligatoire.");
        }
    }

    /** Refuse un nombre négatif. */
    public static void positif(int nombre, String champ) {
        if (nombre < 0) {
            throw new IllegalArgumentException("Le champ " + champ + " doit être positif.");
        }
    }
}

package station.console;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Lecture sécurisée au clavier : une mauvaise saisie redemande la valeur au lieu de planter. */
public class Saisie {

    private final Scanner scanner = new Scanner(System.in);

    /** Lit une ligne ; si le clavier est fermé (fin de saisie), le programme s'arrête proprement. */
    public String lireLigne(String question) {
        System.out.print(question);
        if (!scanner.hasNextLine()) {
            System.out.println("\nFin de la saisie, au revoir.");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /** Lit un texte non vide. */
    public String lireTexte(String question) {
        String texte = lireLigne(question);
        while (texte.isEmpty()) {
            texte = lireLigne("Valeur obligatoire. " + question);
        }
        return texte;
    }

    /** Lit un nombre entier ; redemande tant que la saisie n'est pas un nombre. */
    public int lireEntier(String question) {
        while (true) {
            try {
                return Integer.parseInt(lireLigne(question));
            } catch (NumberFormatException e) {
                System.out.println("Veuillez taper un nombre entier.");
            }
        }
    }

    /** Lit un nombre entier compris entre min et max. */
    public int lireEntierEntre(String question, int min, int max) {
        int nombre = lireEntier(question);
        while (nombre < min || nombre > max) {
            System.out.println("Choisissez un nombre entre " + min + " et " + max + ".");
            nombre = lireEntier(question);
        }
        return nombre;
    }

    /** Lit une réponse o/n. */
    public boolean lireOuiNon(String question) {
        String reponse = lireLigne(question + " (o/n) : ").toLowerCase();
        while (!reponse.equals("o") && !reponse.equals("n")) {
            reponse = lireLigne("Répondez o ou n : ").toLowerCase();
        }
        return reponse.equals("o");
    }

    /** Lit une date au format aaaa-mm-jj. */
    public LocalDate lireDate(String question) {
        while (true) {
            try {
                return LocalDate.parse(lireLigne(question));
            } catch (DateTimeParseException e) {
                System.out.println("Format attendu : aaaa-mm-jj (ex. 2026-11-24).");
            }
        }
    }

    /** Affiche une liste, une ligne par élément, ou un message si elle est vide. */
    public void afficherListe(List<?> liste) {
        if (liste.isEmpty()) {
            System.out.println("(aucun résultat)");
        }
        liste.forEach(System.out::println);
    }
}

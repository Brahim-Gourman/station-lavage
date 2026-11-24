package station.model;

/** Employé qui lave les véhicules. Un laveur qui quitte la station est désactivé. */
public class Laveur {

    private int id;
    private String nom;
    private boolean actif;

    /** Crée un laveur. */
    public Laveur(int id, String nom, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.actif = actif;
    }

    /** Rend le laveur disponible pour travailler. */
    public void activer() {
        actif = true;
    }

    /** Retire le laveur de l'activité, sans effacer son historique. */
    public void desactiver() {
        actif = false;
    }

    /** Identifiant en base. */
    public int getId() {
        return id;
    }

    /** Modifie l'identifiant (donné par la base après l'ajout). */
    public void setId(int id) {
        this.id = id;
    }

    /** Nom du laveur. */
    public String getNom() {
        return nom;
    }

    /** Change le nom. */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** Indique si le laveur est actif. */
    public boolean isActif() {
        return actif;
    }

    /** Texte affiché pour le laveur. */
    @Override
    public String toString() {
        return id + " - " + nom + (actif ? "" : " (inactif)");
    }
}

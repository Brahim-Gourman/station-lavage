package station.model;

import java.util.HashSet;
import java.util.Set;

/** Client de la station, avec ses véhicules (sans doublon grâce au Set). */
public class Client {

    private int id;
    private String nom;
    private String telephone;
    private final Set<Vehicule> vehicules = new HashSet<>();

    /** Crée un client. */
    public Client(int id, String nom, String telephone) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
    }

    /** Ajoute un véhicule ; un véhicule déjà présent n'est pas ajouté deux fois. */
    public void ajouterVehicule(Vehicule vehicule) {
        vehicules.add(vehicule);
    }

    /** Véhicules du client. */
    public Set<Vehicule> getVehicules() {
        return vehicules;
    }

    /** Identifiant en base. */
    public int getId() {
        return id;
    }

    /** Modifie l'identifiant (donné par la base après l'ajout). */
    public void setId(int id) {
        this.id = id;
    }

    /** Nom du client. */
    public String getNom() {
        return nom;
    }

    /** Change le nom. */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** Téléphone (unique). */
    public String getTelephone() {
        return telephone;
    }

    /** Change le téléphone. */
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    /** Texte affiché pour le client. */
    @Override
    public String toString() {
        return id + " - " + nom + " (" + telephone + ")";
    }
}

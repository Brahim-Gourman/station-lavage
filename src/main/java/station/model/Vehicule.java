package station.model;

import java.util.Objects;

/** Véhicule d'un client. Chaque sous-classe calcule son propre prix. */
public abstract class Vehicule {

    private int id;
    private int clientId;
    private String immatriculation;

    /** Crée un véhicule rattaché à un client. */
    public Vehicule(int id, int clientId, String immatriculation) {
        this.id = id;
        this.clientId = clientId;
        this.immatriculation = immatriculation;
    }

    /** Prix du lavage de ce véhicule pour une formule, sans les options. */
    public abstract double calculerPrix(Formule formule);

    /** Type enregistré dans la colonne "type" de la base : MOTO, VOITURE ou CAMION. */
    public abstract String getType();

    /** Indique si la formule est possible pour ce véhicule (toutes par défaut). */
    public boolean accepte(Formule formule) {
        return true;
    }

    /** Identifiant en base. */
    public int getId() {
        return id;
    }

    /** Modifie l'identifiant (donné par la base après l'ajout). */
    public void setId(int id) {
        this.id = id;
    }

    /** Identifiant du client propriétaire. */
    public int getClientId() {
        return clientId;
    }

    /** Change le client propriétaire. */
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    /** Immatriculation (unique). */
    public String getImmatriculation() {
        return immatriculation;
    }

    /** Change l'immatriculation. */
    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    /** Deux véhicules sont égaux s'ils ont la même immatriculation (utile pour le Set du client). */
    @Override
    public boolean equals(Object autre) {
        if (!(autre instanceof Vehicule v)) {
            return false;
        }
        return immatriculation.equals(v.immatriculation);
    }

    /** Code de hachage cohérent avec equals. */
    @Override
    public int hashCode() {
        return Objects.hash(immatriculation);
    }

    /** Texte affiché : type et immatriculation. */
    @Override
    public String toString() {
        return getType() + " " + immatriculation + " (id " + id + ")";
    }
}

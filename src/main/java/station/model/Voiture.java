package station.model;

/** Voiture : prix de base, plus 10 MAD si c'est un SUV. */
public class Voiture extends Vehicule {

    private boolean suv;

    /** Crée une voiture. */
    public Voiture(int id, int clientId, String immatriculation, boolean suv) {
        super(id, clientId, immatriculation);
        this.suv = suv;
    }

    /** Prix de base, + 10 MAD pour un SUV. */
    @Override
    public double calculerPrix(Formule formule) {
        double prix = formule.getPrixBase();
        if (suv) {
            prix = prix + 10;
        }
        return prix;
    }

    /** Type enregistré en base. */
    @Override
    public String getType() {
        return "VOITURE";
    }

    /** Indique si la voiture est un SUV. */
    public boolean isSuv() {
        return suv;
    }

    /** Change l'indication SUV. */
    public void setSuv(boolean suv) {
        this.suv = suv;
    }
}

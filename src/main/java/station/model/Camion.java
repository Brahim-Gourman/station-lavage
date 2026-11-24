package station.model;

/** Camion : prix de base × 1,5, plus 10 MAD par essieu au-delà de 2. */
public class Camion extends Vehicule {

    private int nbEssieux;

    /** Crée un camion. */
    public Camion(int id, int clientId, String immatriculation, int nbEssieux) {
        super(id, clientId, immatriculation);
        this.nbEssieux = nbEssieux;
    }

    /** Prix de base × 1,5, + 10 MAD par essieu supplémentaire. */
    @Override
    public double calculerPrix(Formule formule) {
        int essieuxEnPlus = Math.max(0, nbEssieux - 2);
        return formule.getPrixBase() * 1.5 + 10 * essieuxEnPlus;
    }

    /** Type enregistré en base. */
    @Override
    public String getType() {
        return "CAMION";
    }

    /** Nombre d'essieux. */
    public int getNbEssieux() {
        return nbEssieux;
    }

    /** Change le nombre d'essieux. */
    public void setNbEssieux(int nbEssieux) {
        this.nbEssieux = nbEssieux;
    }
}

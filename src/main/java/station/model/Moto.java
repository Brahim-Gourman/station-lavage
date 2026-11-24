package station.model;

/** Moto : prix de base × 0,7, formule Express uniquement. */
public class Moto extends Vehicule {

    /** Crée une moto. */
    public Moto(int id, int clientId, String immatriculation) {
        super(id, clientId, immatriculation);
    }

    /** Prix de base × 0,7 (calculé × 7 / 10 pour éviter les erreurs d'arrondi). */
    @Override
    public double calculerPrix(Formule formule) {
        return formule.getPrixBase() * 7 / 10;
    }

    /** Une moto accepte seulement la formule Express. */
    @Override
    public boolean accepte(Formule formule) {
        return formule == Formule.EXPRESS;
    }

    /** Type enregistré en base. */
    @Override
    public String getType() {
        return "MOTO";
    }
}
